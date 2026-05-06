package com.bankflow.transaction.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Published to "notification-events" topic for the Notification Service
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private Long transactionId;
    private UUID accountId;
    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remarks;

    @Builder.Default
    private String notificationType = "TRANSACTION_ALERT";

    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    @Builder.Default
    private String source = "transaction-service";
}
