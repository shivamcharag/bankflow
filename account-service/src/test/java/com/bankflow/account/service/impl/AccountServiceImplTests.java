package com.bankflow.account.service.impl;

import com.bankflow.account.dto.request.CreateAccountRequest;
import com.bankflow.account.dto.request.DepositWithdrawRequest;
import com.bankflow.account.dto.response.AccountResponse;
import com.bankflow.account.entity.Account;
import com.bankflow.account.exception.AccountNotFoundException;
import com.bankflow.account.exception.DuplicateAccountException;
import com.bankflow.account.exception.InsufficientBalanceException;
import com.bankflow.account.repository.AccountRepository;
import com.bankflow.account.util.AccountNumberGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// JUnit 5 + Mockito
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl Unit Tests")
class AccountServiceImplTests {

    // ── Mocks — fake versions of dependencies ─────────────────────────────────
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    // ── InjectMocks — creates real AccountServiceImpl with mocks injected ─────
    @InjectMocks
    private AccountServiceImpl accountService;

    // ── Test Data ─────────────────────────────────────────────────────────────
    private Account testAccount;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testAccount = Account.builder()
                .id(testAccountId)
                .accountNumber("BF20260000001001")
                .accountHolderName("Shiva Kumar")
                .email("shiva@bankflow.com")
                .phoneNumber("9876543210")
                .balance(new BigDecimal("5000.00"))
                .accountType(Account.AccountType.SAVINGS)
                .status(Account.AccountStatus.ACTIVE)
                .version(0L)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE ACCOUNT TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createAccount() tests")
    class CreateAccountTests {

        @Test
        @DisplayName("Should create account successfully")
        void shouldCreateAccountSuccessfully() {
            // ARRANGE — set up mocks
            CreateAccountRequest request = CreateAccountRequest.builder()
                    .accountHolderName("Shiva Kumar")
                    .email("shiva@bankflow.com")
                    .phoneNumber("9876543210")
                    .accountType(Account.AccountType.SAVINGS)
                    .initialDeposit(new BigDecimal("5000.00"))
                    .build();

            when(accountRepository.existsByEmail(request.getEmail()))
                    .thenReturn(false);
            when(accountNumberGenerator.generate())
                    .thenReturn("BF20260000001001");
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(testAccount);

            // ACT — call the method
            AccountResponse response = accountService.createAccount(request);

            // ASSERT — verify results
            assertThat(response).isNotNull();
            assertThat(response.accountHolderName()).isEqualTo("Shiva Kumar");
            assertThat(response.balance()).isEqualByComparingTo("5000.00");
            assertThat(response.status()).isEqualTo(Account.AccountStatus.ACTIVE);

            // Verify interactions
            verify(accountRepository).existsByEmail("shiva@bankflow.com");
            verify(accountRepository).save(any(Account.class));
            verify(kafkaTemplate).send(eq("account-events"), anyString(), any());
        }

        @Test
        @DisplayName("Should throw DuplicateAccountException when email exists")
        void shouldThrowExceptionWhenEmailExists() {
            // ARRANGE
            CreateAccountRequest request = CreateAccountRequest.builder()
                    .accountHolderName("Shiva Kumar")
                    .email("shiva@bankflow.com")
                    .phoneNumber("9876543210")
                    .accountType(Account.AccountType.SAVINGS)
                    .initialDeposit(new BigDecimal("5000.00"))
                    .build();

            when(accountRepository.existsByEmail(request.getEmail()))
                    .thenReturn(true); // email already exists!

            // ACT + ASSERT
            assertThatThrownBy(() -> accountService.createAccount(request))
                    .isInstanceOf(DuplicateAccountException.class)
                    .hasMessageContaining("email");

            // Verify save was NEVER called
            verify(accountRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DEPOSIT TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deposit() tests")
    class DepositTests {

        @Test
        @DisplayName("Should deposit successfully and return updated balance")
        void shouldDepositSuccessfully() {
            // ARRANGE
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .amount(new BigDecimal("2000.00"))
                    .remarks("Salary credit")
                    .build();

            Account updatedAccount = Account.builder()
                    .id(testAccountId)
                    .accountNumber("BF20260000001001")
                    .accountHolderName("Shiva Kumar")
                    .email("shiva@bankflow.com")
                    .balance(new BigDecimal("7000.00")) // 5000 + 2000
                    .accountType(Account.AccountType.SAVINGS)
                    .status(Account.AccountStatus.ACTIVE)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(updatedAccount);

            // ACT
            AccountResponse response = accountService.deposit(testAccountId, request);

            // ASSERT
            assertThat(response.balance())
                    .isEqualByComparingTo("7000.00");
            verify(kafkaTemplate).send(eq("transaction-events"), anyString(), any());
        }

        @Test
        @DisplayName("Should throw AccountNotFoundException when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            // ARRANGE
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .amount(new BigDecimal("2000.00"))
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.empty()); // account doesn't exist!

            // ACT + ASSERT
            assertThatThrownBy(() ->
                    accountService.deposit(testAccountId, request))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should still succeed when Kafka publish fails")
        void shouldSucceedEvenWhenKafkaFails() {
            // ARRANGE
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(testAccount);

            // Kafka throws exception
            when(kafkaTemplate.send(any(), any(), any()))
                    .thenThrow(new RuntimeException("Kafka unavailable"));

            // ACT + ASSERT — should NOT throw, Kafka failure is caught internally
            assertThatNoException().isThrownBy(() ->
                    accountService.deposit(testAccountId, request));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WITHDRAW TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("withdraw() tests")
    class WithdrawTests {

        @Test
        @DisplayName("Should withdraw successfully")
        void shouldWithdrawSuccessfully() {
            // ARRANGE
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Account updatedAccount = Account.builder()
                    .id(testAccountId)
                    .balance(new BigDecimal("4000.00")) // 5000 - 1000
                    .status(Account.AccountStatus.ACTIVE)
                    .accountType(Account.AccountType.SAVINGS)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(updatedAccount);

            // ACT
            AccountResponse response = accountService
                    .withdraw(testAccountId, request);

            // ASSERT
            assertThat(response.balance())
                    .isEqualByComparingTo("4000.00");
        }

        @Test
        @DisplayName("Should throw InsufficientBalanceException")
        void shouldThrowInsufficientBalanceException() {
            // ARRANGE — try to withdraw more than balance
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .amount(new BigDecimal("99999.00")) // more than 5000!
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));

            // ACT + ASSERT
            assertThatThrownBy(() ->
                    accountService.withdraw(testAccountId, request))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient balance");

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when withdrawing exact balance amount")
        void shouldAllowWithdrawingExactBalance() {
            // ARRANGE — withdraw exactly the balance
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .amount(new BigDecimal("5000.00")) // exactly the balance
                    .build();

            Account zeroBalanceAccount = Account.builder()
                    .id(testAccountId)
                    .balance(BigDecimal.ZERO)
                    .status(Account.AccountStatus.ACTIVE)
                    .accountType(Account.AccountType.SAVINGS)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(zeroBalanceAccount);

            // ACT + ASSERT — exact amount should be allowed
            assertThatNoException().isThrownBy(() ->
                    accountService.withdraw(testAccountId, request));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET ACCOUNT TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAccount() tests")
    class GetAccountTests {

        @Test
        @DisplayName("Should return account by ID")
        void shouldReturnAccountById() {
            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));

            AccountResponse response = accountService
                    .getAccountById(testAccountId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(testAccountId);
            assertThat(response.email()).isEqualTo("shiva@bankflow.com");
        }

        @Test
        @DisplayName("Should return all accounts mapped to DTOs using Streams")
        void shouldReturnAllAccountsAsResponses() {
            Account account2 = Account.builder()
                    .id(UUID.randomUUID())
                    .accountNumber("BF20260000001002")
                    .accountHolderName("Test User")
                    .email("test@bankflow.com")
                    .balance(new BigDecimal("10000.00"))
                    .accountType(Account.AccountType.CURRENT)
                    .status(Account.AccountStatus.ACTIVE)
                    .build();

            when(accountRepository.findAll())
                    .thenReturn(List.of(testAccount, account2));

            List<AccountResponse> responses = accountService.getAllAccounts();

            assertThat(responses).hasSize(2);
            assertThat(responses)
                    .extracting(AccountResponse::accountType)
                    .containsExactlyInAnyOrder(
                            Account.AccountType.SAVINGS,
                            Account.AccountType.CURRENT
                    );
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE / SOFT DELETE TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteAccount() tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should soft delete — set status to CLOSED not delete from DB")
        void shouldSoftDeleteAccount() {
            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(testAccount);

            accountService.deleteAccount(testAccountId);

            // Verify status was set to CLOSED
            ArgumentCaptor<Account> accountCaptor =
                    ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());

            assertThat(accountCaptor.getValue().getStatus())
                    .isEqualTo(Account.AccountStatus.CLOSED);

            // Verify actual DB delete was NEVER called
            verify(accountRepository, never()).deleteById(any());
        }
    }
}