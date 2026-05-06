package com.bankflow.account.service;

import com.bankflow.account.dto.request.CreateAccountRequest;
import com.bankflow.account.dto.request.DepositWithdrawRequest;
import com.bankflow.account.dto.request.TransferRequest;
import com.bankflow.account.dto.response.AccountResponse;
import com.bankflow.account.entity.Account;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

// Interface — defines the CONTRACT
// Service layer depends on this, not on implementation
// SOLID: D — Dependency Inversion Principle
public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountById(UUID id);

    AccountResponse getAccountByAccountNumber(String accountNumber);

    List<AccountResponse> getAllAccounts();

    List<AccountResponse> getAccountsByStatus(Account.AccountStatus status);

    AccountResponse deposit(UUID id, DepositWithdrawRequest request);

    AccountResponse withdraw(UUID id, DepositWithdrawRequest request);

    AccountResponse transfer(TransferRequest request);

    AccountResponse updateAccountStatus(UUID id, Account.AccountStatus status);

    void deleteAccount(UUID id);

    BigDecimal getBalanceByEmail(String email);
}