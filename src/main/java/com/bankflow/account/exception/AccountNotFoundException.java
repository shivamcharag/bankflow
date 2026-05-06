package com.bankflow.account.exception;

import java.util.UUID;

// Extends RuntimeException — unchecked exception
// No need to declare in method signatures
public class AccountNotFoundException extends RuntimeException {

    private final UUID accountId;
    private final String accountNumber;

    // Constructor 1 — search by UUID
    public AccountNotFoundException(UUID accountId) {
        super("Account not found with id: " + accountId);
        this.accountId = accountId;
        this.accountNumber = null;
    }

    // Constructor 2 — search by account number
    public AccountNotFoundException(String accountNumber) {
        super("Account not found with account number: " + accountNumber);
        this.accountId = null;
        this.accountNumber = accountNumber;
    }

    public UUID getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
}