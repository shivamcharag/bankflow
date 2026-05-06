package com.bankflow.account.dto.request;

import com.bankflow.account.entity.Account;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String accountHolderName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String phoneNumber;

    @NotNull(message = "Account type is required")
    private Account.AccountType accountType;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "500.00", message = "Minimum initial deposit is ₹500")
    private java.math.BigDecimal initialDeposit;
}