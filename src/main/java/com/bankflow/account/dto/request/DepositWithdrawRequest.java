package com.bankflow.account.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositWithdrawRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum amount is ₹1")
    private java.math.BigDecimal amount;

    private String remarks; // optional note
}