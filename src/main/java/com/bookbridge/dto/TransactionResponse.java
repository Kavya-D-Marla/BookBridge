package com.bookbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long transactionId;
    private Long negotiationId;
    private Long bookId;
    private String bookTitle;
    private String buyerName;
    private String sellerName;
    private BigDecimal amount;
    private String paymentStatus;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime transactionDate;
}
