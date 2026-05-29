package com.bookbridge.integration;

import com.bookbridge.dto.PaymentOrderResponse;
import com.bookbridge.entity.Negotiation;

import java.math.BigDecimal;

public interface RazorpayService {
    PaymentOrderResponse createOrder(Negotiation negotiation, BigDecimal amount);
    boolean verifySignature(String orderId, String paymentId, String signature);
    String generateReceipt(Long transactionId);
}
