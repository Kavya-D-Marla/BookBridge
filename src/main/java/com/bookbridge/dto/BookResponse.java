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
public class BookResponse {
    private Long bookId;
    private String title;
    private String author;
    private String edition;
    private Integer publishedYear;
    private String condition;
    private String subject;
    private String description;
    private BigDecimal askingPrice;
    private String status;
    private Long sellerId;
    private String sellerName;
    private List<String> imageUrls;
    private int viewsCount;
    private LocalDateTime createdAt;
}
