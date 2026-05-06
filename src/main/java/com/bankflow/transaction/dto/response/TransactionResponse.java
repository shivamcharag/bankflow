package com.bankflow.transaction.dto.response;

import com.bankflow.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Java Record — immutable, no boilerplate, perfect for responses
public record TransactionResponse(
        Long id,
        UUID accountId,
        String accountNumber,
        Transaction.TransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String remarks,
        Transaction.TransactionStatus status,
        LocalDateTime eventTime,
        LocalDateTime createdAt
) {
    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAccountNumber(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getRemarks(),
                transaction.getStatus(),
                transaction.getEventTime(),
                transaction.getCreatedAt()
        );
    }
}
