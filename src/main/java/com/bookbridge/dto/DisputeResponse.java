package com.bookbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeResponse {
    private Long disputeId;
    private Long transactionId;
    private String bookTitle;
    private String buyerName;
    private String sellerName;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}
