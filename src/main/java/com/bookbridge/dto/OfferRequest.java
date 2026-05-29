package com.bookbridge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OfferRequest {

    @NotNull(message = "Offered price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Offered price must be greater than zero")
    private BigDecimal offeredPrice;

    private String message;
}
