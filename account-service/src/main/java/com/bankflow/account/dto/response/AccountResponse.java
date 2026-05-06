package com.bankflow.account.dto.response;

import com.bankflow.account.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Java Record — immutable, no boilerplate, perfect for responses
// Automatically gets: constructor, getters, equals, hashCode, toString
public record AccountResponse(
        UUID id,
        String accountNumber,
        String accountHolderName,
        String email,
        String phoneNumber,
        BigDecimal balance,
        Account.AccountType accountType,
        Account.AccountStatus status,
        LocalDateTime createdAt
) {
    // Static factory method — converts Entity to DTO
    // This is where mapping happens — no external library needed
    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getEmail(),
                account.getPhoneNumber(),
                account.getBalance(),
                account.getAccountType(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}