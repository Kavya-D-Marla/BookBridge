package com.bookbridge.service;

import com.bookbridge.dto.PaymentOrderResponse;
import com.bookbridge.dto.PaymentVerificationRequest;
import com.bookbridge.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {
    PaymentOrderResponse initiatePayment(Long negotiationId, Long buyerId);
    TransactionResponse verifyPaymentAndCompleteTransaction(Long buyerId, PaymentVerificationRequest request);
    TransactionResponse getTransactionDetails(Long transactionId, Long userId);
    List<TransactionResponse> getMyTransactions(Long userId);
    String downloadReceipt(Long transactionId, Long userId);
}
