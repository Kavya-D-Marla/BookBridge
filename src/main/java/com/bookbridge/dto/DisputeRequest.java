package com.bookbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DisputeRequest {

    @NotNull(message = "Transaction ID is required")
    private Long transactionId;

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotBlank(message = "Detailed description is required")
    private String description;
}
