package com.bankflow.account.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// This is the message that travels through Kafka
// Other services (Transaction, Notification) will receive this
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent {

    private UUID accountId;
    private String accountNumber;
    private String accountHolderName;
    private String email;
    private String accountType;

    @Builder.Default                        // Lombok — default value in Builder
    private LocalDateTime eventTime = LocalDateTime.now();

    @Builder.Default
    private String eventType = "ACCOUNT_CREATED";

    @Builder.Default
    private String source = "account-service";  // which service sent this
}