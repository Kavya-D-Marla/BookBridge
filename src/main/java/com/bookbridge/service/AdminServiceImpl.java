package com.bookbridge.service;

import com.bookbridge.dto.AdminStatsResponse;
import com.bookbridge.dto.BookResponse;
import com.bookbridge.dto.DisputeResponse;
import com.bookbridge.dto.TransactionResponse;
import com.bookbridge.dto.UserResponse;
import com.bookbridge.entity.*;
import com.bookbridge.repository.BookRepository;
import com.bookbridge.repository.DisputeRepository;
import com.bookbridge.repository.TransactionRepository;
import com.bookbridge.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final TransactionRepository transactionRepository;
    private final DisputeRepository disputeRepository;

    @Autowired
    public AdminServiceImpl(UserRepository userRepository,
                            BookRepository bookRepository,
                            TransactionRepository transactionRepository,
                            DisputeRepository disputeRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.transactionRepository = transactionRepository;
        this.disputeRepository = disputeRepository;
    }

    @Override
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalBooks = bookRepository.count();
        
        List<Transaction> transactions = transactionRepository.findAll();
        long totalTransactions = transactions.size();

        BigDecimal totalRevenue = transactions.stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalDisputes = disputeRepository.count();

        // Books by Status breakdown
        Map<String, Long> booksByStatus = new HashMap<>();
        for (BookStatus status : BookStatus.values()) {
            long count = bookRepository.findByStatus(status).size();
            booksByStatus.put(status.name(), count);
        }

        // Disputes by Status breakdown
        Map<String, Long> disputesByStatus = new HashMap<>();
        List<Dispute> disputes = disputeRepository.findAll();
        for (DisputeStatus status : DisputeStatus.values()) {
            long count = disputes.stream().filter(d -> d.getStatus() == status).count();
            disputesByStatus.put(status.name(), count);
        }

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalBooks(totalBooks)
                .totalTransactions(totalTransactions)
                .totalRevenue(totalRevenue)
                .totalDisputes(totalDisputes)
                .booksByStatus(booksByStatus)
                .disputesByStatus(disputesByStatus)
                .build();
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .emailVerified(user.isEmailVerified())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(book -> BookResponse.builder()
                        .bookId(book.getBookId())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .edition(book.getEdition())
                        .publishedYear(book.getPublishedYear())
                        .condition(book.getCondition().name())
                        .subject(book.getSubject())
                        .description(book.getDescription())
                        .askingPrice(book.getAskingPrice())
                        .status(book.getStatus().name())
                        .sellerId(book.getSeller().getUserId())
                        .sellerName(book.getSeller().getFirstName() + " " + book.getSeller().getLastName())
                        .imageUrls(book.getImageUrls())
                        .createdAt(book.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(transaction -> TransactionResponse.builder()
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
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<DisputeResponse> getAllDisputes() {
        return disputeRepository.findAll().stream()
                .map(dispute -> DisputeResponse.builder()
                        .disputeId(dispute.getDisputeId())
                        .transactionId(dispute.getTransaction().getTransactionId())
                        .bookTitle(dispute.getTransaction().getNegotiation().getBook().getTitle())
                        .buyerName(dispute.getTransaction().getBuyer().getFirstName() + " " + dispute.getTransaction().getBuyer().getLastName())
                        .sellerName(dispute.getTransaction().getSeller().getFirstName() + " " + dispute.getTransaction().getSeller().getLastName())
                        .reason(dispute.getReason())
                        .description(dispute.getDescription())
                        .status(dispute.getStatus().name())
                        .createdAt(dispute.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
