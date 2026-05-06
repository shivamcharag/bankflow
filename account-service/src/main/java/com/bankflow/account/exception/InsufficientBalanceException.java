package com.bankflow.account.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(BigDecimal currentBalance,
                                        BigDecimal requestedAmount) {
        super(String.format(
                "Insufficient balance. Current: ₹%.2f, Requested: ₹%.2f",
                currentBalance, requestedAmount
        ));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
}