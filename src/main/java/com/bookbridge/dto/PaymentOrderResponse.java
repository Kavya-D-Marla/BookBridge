package com.bookbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderResponse {
    private Long negotiationId;
    private BigDecimal amount;
    private String razorpayOrderId;
    private String keyId;
    private String currency;
}
