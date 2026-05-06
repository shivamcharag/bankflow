package com.bankflow.transaction.service.impl;

import com.bankflow.transaction.dto.response.TransactionResponse;
import com.bankflow.transaction.entity.Transaction;
import com.bankflow.transaction.event.NotificationEvent;
import com.bankflow.transaction.event.TransactionEvent;
import com.bankflow.transaction.exception.TransactionNotFoundException;
import com.bankflow.transaction.repository.TransactionRepository;
import com.bankflow.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";

    // ── SAVE TRANSACTION (called by Kafka consumer) ───────────────────────────
    @Override
    @Transactional
    public TransactionResponse saveTransaction(TransactionEvent event) {
        log.info("Saving transaction: {} for account: {}",
                event.getTransactionType(), event.getAccountNumber());

        Transaction transaction = Transaction.builder()
                .accountId(event.getAccountId())
                .accountNumber(event.getAccountNumber())
                .transactionType(Transaction.TransactionType.valueOf(event.getTransactionType()))
                .amount(event.getAmount())
                .balanceAfter(event.getBalanceAfter())
                .remarks(event.getRemarks())
                .status(Transaction.TransactionStatus.COMPLETED)
                .eventTime(event.getEventTime())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction saved with id: {}", saved.getId());

        publishNotificationEvent(saved);

        return TransactionResponse.fromEntity(saved);
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        log.info("Fetching transaction by id: {}", id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return TransactionResponse.fromEntity(transaction);
    }

    // ── GET BY ACCOUNT ID ─────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccountId(UUID accountId) {
        log.info("Fetching transactions for accountId: {}", accountId);
        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── GET BY ACCOUNT NUMBER ─────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccountNumber(String accountNumber) {
        log.info("Fetching transactions for accountNumber: {}", accountNumber);
        return transactionRepository
                .findByAccountNumber(accountNumber)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        log.info("Fetching all transactions");
        return transactionRepository.findAll()
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── GET BY TYPE ───────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByType(Transaction.TransactionType type) {
        log.info("Fetching transactions of type: {}", type);
        return transactionRepository.findByTransactionType(type)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private void publishNotificationEvent(Transaction transaction) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .transactionId(transaction.getId())
                    .accountId(transaction.getAccountId())
                    .accountNumber(transaction.getAccountNumber())
                    .transactionType(transaction.getTransactionType().name())
                    .amount(transaction.getAmount())
                    .balanceAfter(transaction.getBalanceAfter())
                    .remarks(transaction.getRemarks())
                    .build();

            kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC,
                    transaction.getAccountId().toString(), event);
            log.info("Published notification event for transaction: {}", transaction.getId());
        } catch (Exception e) {
            log.error("Failed to publish notification event: {}", e.getMessage());
        }
    }
}
