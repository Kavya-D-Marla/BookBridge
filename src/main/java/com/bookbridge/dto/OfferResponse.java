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
public class OfferResponse {
    private Long offerId;
    private Long negotiationId;
    private Long userId;
    private String userName;
    private BigDecimal offeredPrice;
    private String message;
    private LocalDateTime timestamp;
}
