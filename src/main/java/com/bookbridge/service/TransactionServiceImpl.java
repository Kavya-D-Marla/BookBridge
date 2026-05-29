package com.bookbridge.service;

import com.bookbridge.dto.PaymentOrderResponse;
import com.bookbridge.dto.PaymentVerificationRequest;
import com.bookbridge.dto.TransactionResponse;
import com.bookbridge.entity.*;
import com.bookbridge.exception.BadRequestException;
import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.exception.UnauthorizedException;
import com.bookbridge.integration.RazorpayService;
import com.bookbridge.repository.BookRepository;
import com.bookbridge.repository.NegotiationRepository;
import com.bookbridge.repository.TransactionRepository;
import com.bookbridge.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final NegotiationRepository negotiationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final RazorpayService razorpayService;

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  NegotiationRepository negotiationRepository,
                                  BookRepository bookRepository,
                                  UserRepository userRepository,
                                  RazorpayService razorpayService) {
        this.transactionRepository = transactionRepository;
        this.negotiationRepository = negotiationRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.razorpayService = razorpayService;
    }

    @Override
    @Transactional
    public PaymentOrderResponse initiatePayment(Long negotiationId, Long buyerId) {
        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (negotiation.getStatus() != NegotiationStatus.ACCEPTED) {
            throw new BadRequestException("You can only initiate payment after the negotiation offer has been ACCEPTED.");
        }

        Book book = negotiation.getBook();
        if (book.getStatus() == BookStatus.SOLD) {
            throw new BadRequestException("This textbook has already been sold to another user.");
        }

        if (!negotiation.getBuyer().getUserId().equals(buyerId)) {
            throw new UnauthorizedException("Only the accepted buyer can initiate payment for this deal.");
        }

        // Agreed amount is the price of the last offer in negotiation
        if (negotiation.getOffers().isEmpty()) {
            throw new BadRequestException("Negotiation has no bidding history.");
        }
        Offer finalOffer = negotiation.getOffers().get(negotiation.getOffers().size() - 1);
        BigDecimal agreedAmount = finalOffer.getOfferedPrice();

        // Generate Razorpay Order
        PaymentOrderResponse orderResponse = razorpayService.createOrder(negotiation, agreedAmount);

        // Create transaction logs
        Transaction transaction = Transaction.builder()
                .negotiation(negotiation)
                .buyer(negotiation.getBuyer())
                .seller(negotiation.getSeller())
                .amount(agreedAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .razorpayOrderId(orderResponse.getRazorpayOrderId())
                .build();

        transactionRepository.save(transaction);
        log.info("Initialized checkout order ID: {} for deal ID: {}", 
                orderResponse.getRazorpayOrderId(), negotiationId);

        return orderResponse;
    }

    @Override
    @Transactional
    public TransactionResponse verifyPaymentAndCompleteTransaction(Long buyerId, PaymentVerificationRequest request) {
        Transaction transaction = transactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction record not found for order: " + request.getRazorpayOrderId()));

        if (!transaction.getBuyer().getUserId().equals(buyerId)) {
            throw new UnauthorizedException("You are not authorized to complete this payment.");
        }

        if (transaction.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("This transaction has already been successfully processed.");
        }

        // Verify Signature
        boolean isVerified = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        Book book = transaction.getNegotiation().getBook();

        if (isVerified) {
            transaction.setPaymentStatus(PaymentStatus.SUCCESS);
            transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
            transaction.setRazorpaySignature(request.getRazorpaySignature());

            // Mark textbook as sold
            book.setStatus(BookStatus.SOLD);
            bookRepository.save(book);

            // Auto-Expire all other active negotiations for this specific textbook
            List<Negotiation> competingNegotiations = negotiationRepository.findByBook(book);
            for (Negotiation competing : competingNegotiations) {
                if (!competing.getNegotiationId().equals(transaction.getNegotiation().getNegotiationId()) 
                    && competing.getStatus() == NegotiationStatus.OPEN) {
                    competing.setStatus(NegotiationStatus.EXPIRED);
                    negotiationRepository.save(competing);
                    log.info("Expired competing negotiation thread ID: {} due to book sale.", competing.getNegotiationId());
                }
            }

            log.info("Payment SUCCESS for order ID: {}. Textbook sold!", request.getRazorpayOrderId());
        } else {
            transaction.setPaymentStatus(PaymentStatus.FAILED);
            
            // Revert book to available status so others can buy/bid
            book.setStatus(BookStatus.AVAILABLE);
            bookRepository.save(book);

            log.error("Payment FAILURE verified for order ID: {}. Reverted listing status.", request.getRazorpayOrderId());
        }

        Transaction completedTransaction = transactionRepository.save(transaction);
        return mapToTransactionResponse(completedTransaction);
    }

    @Override
    public TransactionResponse getTransactionDetails(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        User user = userRepository.findById(userId).get();

        if (!transaction.getBuyer().getUserId().equals(userId) && 
            !transaction.getSeller().getUserId().equals(userId) &&
            !user.getRole().name().equals("ROLE_ADMIN")) {
            throw new UnauthorizedException("You do not have permission to view this transaction.");
        }

        return mapToTransactionResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getMyTransactions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Transaction> transactions = new ArrayList<>();
        transactions.addAll(transactionRepository.findByBuyer(user));
        transactions.addAll(transactionRepository.findBySeller(user));

        return transactions.stream()
                .distinct()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String downloadReceipt(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        if (!transaction.getBuyer().getUserId().equals(userId) && 
            !transaction.getSeller().getUserId().equals(userId) &&
            !userRepository.findById(userId).get().getRole().name().equals("ROLE_ADMIN")) {
            throw new UnauthorizedException("Unauthorized access to transaction receipt.");
        }

        if (transaction.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Receipt cannot be generated for pending or failed transactions.");
        }

        return razorpayService.generateReceipt(transactionId);
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .negotiationId(transaction.getNegotiation().getNegotiationId())
                .bookId(transaction.getNegotiation().getBook().getBookId())
                .bookTitle(transaction.getNegotiation().getBook().getTitle())
                .buyerName(transaction.getBuyer().getFirstName() + " " + transaction.getBuyer().getLastName())
                .sellerName(transaction.getSeller().getFirstName() + " " + transaction.getSeller().getLastName())
                .amount(transaction.getAmount())
                .paymentStatus(transaction.getPaymentStatus().name())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .razorpayPaymentId(transaction.getRazorpayPaymentId())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }
}
