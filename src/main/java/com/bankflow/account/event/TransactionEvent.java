package com.bankflow.account.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    private UUID accountId;
    private String accountNumber;
    private String transactionType;     // DEPOSIT or WITHDRAWAL
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remarks;

    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    @Builder.Default
    private String eventType = "TRANSACTION_COMPLETED";

    @Builder.Default
    private String source = "account-service";
}