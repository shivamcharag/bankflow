package com.bankflow.account.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Record for transferring funds between accounts.
 * Uses Jakarta validation for non-null and positive amount validation.
 */
public record TransferRequest(
    @NotNull(message = "From account ID is required")
    UUID fromAccountId,

    @NotNull(message = "To account ID is required")
    UUID toAccountId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    BigDecimal amount
) {}

