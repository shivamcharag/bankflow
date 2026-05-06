package com.bankflow.transaction.consumer;

import com.bankflow.transaction.event.TransactionEvent;
import com.bankflow.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionService transactionService;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleTransactionEvent(@Payload TransactionEvent event) {
        log.info("Received transaction event: type={}, account={}, amount={}",
                event.getTransactionType(), event.getAccountNumber(), event.getAmount());
        try {
            transactionService.saveTransaction(event);
            log.info("Transaction event processed successfully for account: {}",
                    event.getAccountNumber());
        } catch (Exception e) {
            // Log but don't rethrow — prevents consumer from crashing on a bad message
            log.error("Failed to process transaction event for account {}: {}",
                    event.getAccountNumber(), e.getMessage(), e);
        }
    }
}
