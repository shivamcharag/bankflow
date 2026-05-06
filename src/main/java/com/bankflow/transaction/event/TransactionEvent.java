package com.bankflow.transaction.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Mirrors the event published by account-service to the "transaction-events" topic
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {

    private UUID accountId;
    private String accountNumber;
    private String transactionType;     // "DEPOSIT" or "WITHDRAWAL" (String from account-service)
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remarks;
    private LocalDateTime eventTime;
    private String eventType;
    private String source;
}
