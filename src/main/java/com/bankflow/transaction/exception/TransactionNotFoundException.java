package com.bankflow.transaction.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    private final Long transactionId;
    private final UUID accountId;

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found with id: " + transactionId);
        this.transactionId = transactionId;
        this.accountId = null;
    }

    public TransactionNotFoundException(UUID accountId) {
        super("No transactions found for account: " + accountId);
        this.transactionId = null;
        this.accountId = accountId;
    }

    public Long getTransactionId() { return transactionId; }
    public UUID getAccountId() { return accountId; }
}
