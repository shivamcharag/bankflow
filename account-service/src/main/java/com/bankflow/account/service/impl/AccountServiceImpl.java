package com.bankflow.account.service.impl;

import com.bankflow.account.dto.request.CreateAccountRequest;
import com.bankflow.account.dto.request.DepositWithdrawRequest;
import com.bankflow.account.dto.request.TransferRequest;
import com.bankflow.account.dto.response.AccountResponse;
import com.bankflow.account.entity.Account;
import com.bankflow.account.event.AccountCreatedEvent;
import com.bankflow.account.event.TransactionEvent;
import com.bankflow.account.exception.AccountNotFoundException;
import com.bankflow.account.exception.DuplicateAccountException;
import com.bankflow.account.exception.InsufficientBalanceException;
import com.bankflow.account.repository.AccountRepository;
import com.bankflow.account.service.AccountService;
import com.bankflow.account.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service                        // Marks as Spring Bean
@RequiredArgsConstructor        // Lombok — constructor injection for all final fields
public class AccountServiceImpl implements AccountService {

    // ── Dependencies injected via constructor (best practice) ─────────────────
    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AccountNumberGenerator accountNumberGenerator;

    private static final String ACCOUNT_EVENTS_TOPIC = "account-events";

    // ── CREATE ACCOUNT ────────────────────────────────────────────────────────
    @Override
    @Transactional                  // rolls back DB if anything fails
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for email: {}", request.getEmail());

        // 1. Check duplicates
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateAccountException("email", request.getEmail());
        }

        // 2. Build entity using Builder pattern
        Account account = Account.builder()
                .accountNumber(accountNumberGenerator.generate())
                .accountHolderName(request.getAccountHolderName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .balance(request.getInitialDeposit())
                .accountType(request.getAccountType())
                .status(Account.AccountStatus.ACTIVE)
                .build();

        // 3. Save to DB
        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully: {}", savedAccount.getAccountNumber());

        // 4. Publish Kafka event
        publishAccountCreatedEvent(savedAccount);

        // 5. Return DTO — never return entity directly!
        return AccountResponse.fromEntity(savedAccount);
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)     // readOnly = true → performance optimization
    public AccountResponse getAccountById(UUID id) {
        log.info("Fetching account by id: {}", id);
        Account account = findAccountById(id);
        return AccountResponse.fromEntity(account);
    }

    // ── GET BY ACCOUNT NUMBER ─────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account by number: {}", accountNumber);
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return AccountResponse.fromEntity(account);
    }

    // ── GET ALL ACCOUNTS — uses Java Streams ──────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        log.info("Fetching all accounts");
        return accountRepository.findAll()
                .stream()                                    // Stream<Account>
                .map(AccountResponse::fromEntity)            // Stream<AccountResponse>
                .collect(Collectors.toList());               // List<AccountResponse>
    }

    // ── GET BY STATUS — uses Streams + filter ─────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByStatus(Account.AccountStatus status) {
        return accountRepository.findByStatus(status)
                .stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalanceByEmail(String email) {
        log.info("Fetching balance for email: {}", email);
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException(email));
        return account.getBalance();
    }

    // ── DEPOSIT ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountResponse deposit(UUID id, DepositWithdrawRequest request) {
        log.info("Depositing ₹{} to account: {}", request.getAmount(), id);

        Account account = findAccountById(id);

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        // Add balance — BigDecimal is immutable, add() returns new object
        account.setBalance(account.getBalance().add(request.getAmount()));

        Account updatedAccount = accountRepository.save(account);
        log.info("Deposit successful. New balance: {}", updatedAccount.getBalance());

        // Publish transaction event to Kafka
        publishTransactionEvent(updatedAccount, "DEPOSIT", request.getAmount());

        return AccountResponse.fromEntity(updatedAccount);
    }

    // ── WITHDRAW ──────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountResponse withdraw(UUID id, DepositWithdrawRequest request) {
        log.info("Withdrawing ₹{} from account: {}", request.getAmount(), id);

        Account account = findAccountById(id);

        // Check sufficient balance
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    account.getBalance(),
                    request.getAmount()
            );
        }

        // Subtract balance
        account.setBalance(account.getBalance().subtract(request.getAmount()));

        Account updatedAccount = accountRepository.save(account);
        log.info("Withdrawal successful. New balance: {}", updatedAccount.getBalance());

        // Publish transaction event to Kafka
        publishTransactionEvent(updatedAccount, "WITHDRAWAL", request.getAmount());

        return AccountResponse.fromEntity(updatedAccount);
    }

    //

    // ── TRANSFER ──────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountResponse transfer(TransferRequest request) {
        log.info("Transferring ₹{} from account {} to account {}",
                request.amount(), request.fromAccountId(), request.toAccountId());

        // 1. Fetch both accounts — throws AccountNotFoundException if not found
        Account fromAccount = findAccountById(request.fromAccountId());
        Account toAccount = findAccountById(request.toAccountId());

        // 2. Validate both accounts are active
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Source account is not active");
        }
        if (toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Destination account is not active");
        }

        // 3. Check source account has sufficient balance
        if (fromAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(
                    fromAccount.getBalance(),
                    request.amount()
            );
        }

        // 4. Update both balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));

        // 5. Save both accounts to database — happens atomically within transaction
        Account updatedFromAccount = accountRepository.save(fromAccount);
        Account updatedToAccount = accountRepository.save(toAccount);
        log.info("Transfer successful. From account new balance: {}, To account new balance: {}",
                updatedFromAccount.getBalance(), updatedToAccount.getBalance());

        // 6. Publish two TransactionEvents to Kafka
        publishTransactionEvent(updatedFromAccount, "TRANSFER_OUT", request.amount());
        publishTransactionEvent(updatedToAccount, "TRANSFER_IN", request.amount());

        // 7. Return the updated source account
        return AccountResponse.fromEntity(updatedFromAccount);
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountResponse updateAccountStatus(UUID id, Account.AccountStatus status) {
        log.info("Updating status to {} for account: {}", status, id);
        Account account = findAccountById(id);
        account.setStatus(status);
        return AccountResponse.fromEntity(accountRepository.save(account));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteAccount(UUID id) {
        log.info("Deleting account: {}", id);
        Account account = findAccountById(id);
        account.setStatus(Account.AccountStatus.CLOSED);    // soft delete!
        accountRepository.save(account);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private Account findAccountById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private void publishAccountCreatedEvent(Account account) {
        try {
            AccountCreatedEvent event = AccountCreatedEvent.builder()
                    .accountId(account.getId())
                    .accountNumber(account.getAccountNumber())
                    .email(account.getEmail())
                    .accountHolderName(account.getAccountHolderName())
                    .accountType(account.getAccountType().name())
                    .build();

            kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC,
                    account.getId().toString(), event);
            log.info("Published account created event for: {}",
                    account.getAccountNumber());
        } catch (Exception e) {
            // Log but don't fail — Kafka failure shouldn't fail account creation
            log.error("Failed to publish account created event: {}", e.getMessage());
        }
    }

    private void publishTransactionEvent(Account account,
                                         String type,
                                         BigDecimal amount) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .accountId(account.getId())
                    .accountNumber(account.getAccountNumber())
                    .transactionType(type)
                    .amount(amount)
                    .balanceAfter(account.getBalance())
                    .build();

            kafkaTemplate.send("transaction-events",
                    account.getId().toString(),
                    event);
            log.info("Published transaction event: {} for account: {}",
                    type, account.getAccountNumber());
        } catch (Exception e) {
            log.error("Failed to publish transaction event: {}", e.getMessage());
        }
    }
}