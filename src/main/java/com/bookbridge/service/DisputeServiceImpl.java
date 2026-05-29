package com.bookbridge.service;

import com.bookbridge.dto.DisputeRequest;
import com.bookbridge.dto.DisputeResponse;
import com.bookbridge.entity.*;
import com.bookbridge.exception.BadRequestException;
import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.exception.UnauthorizedException;
import com.bookbridge.repository.DisputeRepository;
import com.bookbridge.repository.TransactionRepository;
import com.bookbridge.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public DisputeServiceImpl(DisputeRepository disputeRepository,
                              TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.disputeRepository = disputeRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public DisputeResponse raiseDispute(DisputeRequest request, Long buyerId) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", request.getTransactionId()));

        if (!transaction.getBuyer().getUserId().equals(buyerId)) {
            throw new UnauthorizedException("Only the buyer of the textbook can open a dispute claim.");
        }

        if (transaction.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("You can only open disputes for completed transactions with successful payments.");
        }

        Dispute dispute = Dispute.builder()
                .transaction(transaction)
                .reason(request.getReason().trim())
                .description(request.getDescription().trim())
                .status(DisputeStatus.OPEN)
                .build();

        Dispute savedDispute = disputeRepository.save(dispute);
        log.info("Dispute claim opened for transaction ID: {} by buyer: {}", 
                request.getTransactionId(), transaction.getBuyer().getEmail());

        return mapToDisputeResponse(savedDispute);
    }

    @Override
    @Transactional
    public DisputeResponse updateDisputeStatus(Long disputeId, String status, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin profile not found"));

        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("Access Denied: Only platform administrators can resolve dispute claims.");
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute case", "id", disputeId));

        DisputeStatus disputeStatus;
        try {
            disputeStatus = DisputeStatus.valueOf(status.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid dispute status option: " + status);
        }

        dispute.setStatus(disputeStatus);
        
        // If dispute is resolved, we might refund (mock/log refund)
        if (disputeStatus == DisputeStatus.RESOLVED) {
            log.info("[MOCK REFUND INITIATED] Releasing refunds to buyer: {} for book transaction ID: {}", 
                    dispute.getTransaction().getBuyer().getEmail(), dispute.getTransaction().getTransactionId());
            dispute.getTransaction().setPaymentStatus(PaymentStatus.REFUNDED);
            transactionRepository.save(dispute.getTransaction());
        }

        Dispute updatedDispute = disputeRepository.save(dispute);
        log.info("Dispute case ID: {} updated to status: {} by admin: {}", 
                disputeId, disputeStatus, admin.getEmail());

        return mapToDisputeResponse(updatedDispute);
    }

    @Override
    public List<DisputeResponse> getDisputeHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Dispute> disputes = disputeRepository.findAll();
        List<Dispute> filteredDisputes;

        if (user.getRole() == Role.ROLE_ADMIN) {
            filteredDisputes = disputes;
        } else {
            // Non-admins only see disputes where they are either buyer or seller
            filteredDisputes = disputes.stream()
                .filter(d -> d.getTransaction().getBuyer().getUserId().equals(userId) ||
                             d.getTransaction().getSeller().getUserId().equals(userId))
                .collect(Collectors.toList());
        }

        return filteredDisputes.stream()
                .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
                .map(this::mapToDisputeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DisputeResponse getDisputeDetails(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute case", "id", disputeId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isBuyer = dispute.getTransaction().getBuyer().getUserId().equals(userId);
        boolean isSeller = dispute.getTransaction().getSeller().getUserId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to view this dispute case.");
        }

        return mapToDisputeResponse(dispute);
    }

    private DisputeResponse mapToDisputeResponse(Dispute dispute) {
        return DisputeResponse.builder()
                .disputeId(dispute.getDisputeId())
                .transactionId(dispute.getTransaction().getTransactionId())
                .bookTitle(dispute.getTransaction().getNegotiation().getBook().getTitle())
                .buyerName(dispute.getTransaction().getBuyer().getFirstName() + " " + dispute.getTransaction().getBuyer().getLastName())
                .sellerName(dispute.getTransaction().getSeller().getFirstName() + " " + dispute.getTransaction().getSeller().getLastName())
                .reason(dispute.getReason())
                .description(dispute.getDescription())
                .status(dispute.getStatus().name())
                .createdAt(dispute.getCreatedAt())
                .build();
    }
}
