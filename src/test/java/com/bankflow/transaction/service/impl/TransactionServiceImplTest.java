package com.bankflow.transaction.service.impl;

import com.bankflow.transaction.dto.response.TransactionResponse;
import com.bankflow.transaction.entity.Transaction;
import com.bankflow.transaction.event.TransactionEvent;
import com.bankflow.transaction.exception.TransactionNotFoundException;
import com.bankflow.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImpl Unit Tests")
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Transaction testTransaction;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testTransaction = Transaction.builder()
                .id(1L)
                .accountId(testAccountId)
                .accountNumber("BF20260000001001")
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(new BigDecimal("2000.00"))
                .balanceAfter(new BigDecimal("7000.00"))
                .remarks("Salary credit")
                .status(Transaction.TransactionStatus.COMPLETED)
                .eventTime(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SAVE TRANSACTION TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("saveTransaction() tests")
    class SaveTransactionTests {

        @Test
        @DisplayName("Should save transaction and return response")
        void shouldSaveTransactionSuccessfully() {
            // ARRANGE
            TransactionEvent event = TransactionEvent.builder()
                    .accountId(testAccountId)
                    .accountNumber("BF20260000001001")
                    .transactionType("DEPOSIT")
                    .amount(new BigDecimal("2000.00"))
                    .balanceAfter(new BigDecimal("7000.00"))
                    .remarks("Salary credit")
                    .eventTime(LocalDateTime.now())
                    .build();

            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(testTransaction);

            // ACT
            TransactionResponse response = transactionService.saveTransaction(event);

            // ASSERT
            assertThat(response).isNotNull();
            assertThat(response.accountId()).isEqualTo(testAccountId);
            assertThat(response.transactionType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
            assertThat(response.amount()).isEqualByComparingTo("2000.00");
            assertThat(response.status()).isEqualTo(Transaction.TransactionStatus.COMPLETED);

            verify(transactionRepository).save(any(Transaction.class));
            verify(kafkaTemplate).send(eq("notification-events"), anyString(), any());
        }

        @Test
        @DisplayName("Should publish notification event after saving")
        void shouldPublishNotificationEvent() {
            // ARRANGE
            TransactionEvent event = TransactionEvent.builder()
                    .accountId(testAccountId)
                    .accountNumber("BF20260000001001")
                    .transactionType("WITHDRAWAL")
                    .amount(new BigDecimal("1000.00"))
                    .balanceAfter(new BigDecimal("4000.00"))
                    .eventTime(LocalDateTime.now())
                    .build();

            Transaction withdrawalTx = Transaction.builder()
                    .id(2L)
                    .accountId(testAccountId)
                    .accountNumber("BF20260000001001")
                    .transactionType(Transaction.TransactionType.WITHDRAWAL)
                    .amount(new BigDecimal("1000.00"))
                    .balanceAfter(new BigDecimal("4000.00"))
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .eventTime(LocalDateTime.now())
                    .build();

            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(withdrawalTx);

            // ACT
            transactionService.saveTransaction(event);

            // ASSERT — verify notification was sent
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(kafkaTemplate).send(eq("notification-events"), anyString(), eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("Should still save transaction when Kafka publish fails")
        void shouldSucceedEvenWhenKafkaFails() {
            // ARRANGE
            TransactionEvent event = TransactionEvent.builder()
                    .accountId(testAccountId)
                    .accountNumber("BF20260000001001")
                    .transactionType("DEPOSIT")
                    .amount(new BigDecimal("500.00"))
                    .balanceAfter(new BigDecimal("5500.00"))
                    .eventTime(LocalDateTime.now())
                    .build();

            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(testTransaction);
            when(kafkaTemplate.send(any(), any(), any()))
                    .thenThrow(new RuntimeException("Kafka unavailable"));

            // ACT + ASSERT — Kafka failure should not fail the DB save
            assertThatNoException().isThrownBy(() ->
                    transactionService.saveTransaction(event));

            verify(transactionRepository).save(any(Transaction.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET TRANSACTION TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTransaction() tests")
    class GetTransactionTests {

        @Test
        @DisplayName("Should return transaction by ID")
        void shouldReturnTransactionById() {
            when(transactionRepository.findById(1L))
                    .thenReturn(Optional.of(testTransaction));

            TransactionResponse response = transactionService.getTransactionById(1L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.accountNumber()).isEqualTo("BF20260000001001");
        }

        @Test
        @DisplayName("Should throw TransactionNotFoundException when ID not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            when(transactionRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionById(999L))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Should return all transactions for an account ID")
        void shouldReturnTransactionsByAccountId() {
            Transaction tx2 = Transaction.builder()
                    .id(2L)
                    .accountId(testAccountId)
                    .accountNumber("BF20260000001001")
                    .transactionType(Transaction.TransactionType.WITHDRAWAL)
                    .amount(new BigDecimal("500.00"))
                    .balanceAfter(new BigDecimal("6500.00"))
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .eventTime(LocalDateTime.now())
                    .build();

            when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(testAccountId))
                    .thenReturn(List.of(testTransaction, tx2));

            List<TransactionResponse> responses =
                    transactionService.getTransactionsByAccountId(testAccountId);

            assertThat(responses).hasSize(2);
            assertThat(responses)
                    .extracting(TransactionResponse::transactionType)
                    .containsExactlyInAnyOrder(
                            Transaction.TransactionType.DEPOSIT,
                            Transaction.TransactionType.WITHDRAWAL
                    );
        }

        @Test
        @DisplayName("Should return empty list when account has no transactions")
        void shouldReturnEmptyListWhenNoTransactions() {
            UUID unknownAccountId = UUID.randomUUID();
            when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(unknownAccountId))
                    .thenReturn(List.of());

            List<TransactionResponse> responses =
                    transactionService.getTransactionsByAccountId(unknownAccountId);

            assertThat(responses).isEmpty();
            verify(transactionRepository).findByAccountIdOrderByCreatedAtDesc(unknownAccountId);
        }

        @Test
        @DisplayName("Should return all transactions mapped to DTOs using Streams")
        void shouldReturnAllTransactions() {
            when(transactionRepository.findAll())
                    .thenReturn(List.of(testTransaction));

            List<TransactionResponse> responses = transactionService.getAllTransactions();

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).id()).isEqualTo(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET BY TYPE TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTransactionsByType() tests")
    class GetByTypeTests {

        @Test
        @DisplayName("Should return only DEPOSIT transactions")
        void shouldReturnOnlyDepositTransactions() {
            when(transactionRepository.findByTransactionType(Transaction.TransactionType.DEPOSIT))
                    .thenReturn(List.of(testTransaction));

            List<TransactionResponse> responses =
                    transactionService.getTransactionsByType(Transaction.TransactionType.DEPOSIT);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).transactionType())
                    .isEqualTo(Transaction.TransactionType.DEPOSIT);
        }
    }
}
