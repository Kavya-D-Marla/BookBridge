package com.bookbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NegotiationResponse {
    private Long negotiationId;
    private Long bookId;
    private String bookTitle;
    private BigDecimal askingPrice;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private String status;
    private List<OfferResponse> offers;
    private LocalDateTime createdAt;
}
