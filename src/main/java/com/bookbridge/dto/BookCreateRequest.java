package com.bookbridge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BookCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    private String edition;

    @NotNull(message = "Published year is required")
    private Integer publishedYear;

    @NotBlank(message = "Book condition is required (NEW, LIKE_NEW, GOOD, ACCEPTABLE)")
    private String condition;

    private String subject;

    private String description;

    @NotNull(message = "Asking price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Asking price must be greater than zero")
    private BigDecimal askingPrice;

    private List<String> imageUrls;
}
