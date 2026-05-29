package com.bookbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalBooks;
    private long totalTransactions;
    private BigDecimal totalRevenue;
    private long totalDisputes;
    private Map<String, Long> booksByStatus;
    private Map<String, Long> disputesByStatus;
}
