package com.bankflow.transaction.repository;

import com.bankflow.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ── Way 1: Derived Query Methods ──────────────────────────────────────────

    List<Transaction> findByAccountId(UUID accountId);

    List<Transaction> findByAccountNumber(String accountNumber);

    List<Transaction> findByTransactionType(Transaction.TransactionType transactionType);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<Transaction> findByAccountIdAndTransactionType(
            UUID accountId,
            Transaction.TransactionType transactionType
    );

    long countByAccountId(UUID accountId);

    // ── Way 2: JPQL Query ─────────────────────────────────────────────────────

    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
            "AND t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<Transaction> findRecentByAccountId(
            @Param("accountId") UUID accountId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.accountId = :accountId " +
            "AND t.transactionType = :type AND t.status = 'COMPLETED'")
    BigDecimal sumAmountByAccountIdAndType(
            @Param("accountId") UUID accountId,
            @Param("type") Transaction.TransactionType type
    );

    // ── Way 3: Native SQL Query ───────────────────────────────────────────────

    @Query(value = "SELECT * FROM transactions WHERE status = 'COMPLETED' " +
            "ORDER BY created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<Transaction> findRecentCompletedTransactions(@Param("limit") int limit);

    // ── Way 4: Projection — only fetch summary fields ─────────────────────────

    @Query("SELECT t.accountNumber as accountNumber, t.transactionType as transactionType, " +
            "t.amount as amount, t.createdAt as createdAt " +
            "FROM Transaction t WHERE t.accountId = :accountId ORDER BY t.createdAt DESC")
    List<TransactionSummaryProjection> findTransactionSummaryByAccountId(
            @Param("accountId") UUID accountId
    );

    // ── Projection Interface ──────────────────────────────────────────────────

    interface TransactionSummaryProjection {
        String getAccountNumber();
        Transaction.TransactionType getTransactionType();
        BigDecimal getAmount();
        LocalDateTime getCreatedAt();
    }
}
