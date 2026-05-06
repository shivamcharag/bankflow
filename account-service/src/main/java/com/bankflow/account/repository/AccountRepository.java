package com.bankflow.account.repository;

import com.bankflow.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository
        extends JpaRepository<Account, UUID>,          // gives us save, findById, findAll, delete
        JpaSpecificationExecutor<Account> {    // gives us Specification support

    // ── Way 1: Derived Query Methods ─────────────────────────────────────────

    Optional<Account> findByEmail(String email);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByStatus(Account.AccountStatus status);

    List<Account> findByAccountTypeAndStatus(
            Account.AccountType accountType,
            Account.AccountStatus status
    );

    // ── Way 2: JPQL Query ─────────────────────────────────────────────────────

    @Query("SELECT a FROM Account a WHERE a.balance >= :minBalance " +
            "AND a.status = 'ACTIVE' ORDER BY a.balance DESC")
    List<Account> findActiveAccountsWithMinBalance(
            @Param("minBalance") BigDecimal minBalance
    );

    // ── Way 3: Native SQL Query ───────────────────────────────────────────────

    @Query(value = "SELECT * FROM accounts WHERE status = 'ACTIVE' " +
            "ORDER BY created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<Account> findRecentActiveAccounts(@Param("limit") int limit);

    // ── Way 4: Projection — only fetch what you need ──────────────────────────

    @Query("SELECT a.accountNumber as accountNumber, " +
            "a.balance as balance, " +
            "a.accountHolderName as accountHolderName " +
            "FROM Account a WHERE a.status = 'ACTIVE'")
    List<AccountSummaryProjection> findAllAccountSummaries();

    // ── Way 5: @Modifying — for UPDATE/DELETE queries ─────────────────────────

    @Modifying
    @Query("UPDATE Account a SET a.status = :status WHERE a.id = :id")
    int updateAccountStatus(
            @Param("id") UUID id,
            @Param("status") Account.AccountStatus status
    );

    // ── Projection Interface (defined here, inside repository file) ───────────

    interface AccountSummaryProjection {
        String getAccountNumber();
        BigDecimal getBalance();
        String getAccountHolderName();
    }
}