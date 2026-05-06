package com.bankflow.account.controller;

import com.bankflow.account.dto.request.CreateAccountRequest;
import com.bankflow.account.dto.request.DepositWithdrawRequest;
import com.bankflow.account.dto.response.AccountResponse;
import com.bankflow.account.dto.response.ApiResponse;
import com.bankflow.account.entity.Account;
import com.bankflow.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController                         // @Controller + @ResponseBody combined
@RequestMapping("/api/v1/accounts")     // base URL for all endpoints
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;  // depends on interface, not impl!

    // ── CREATE ACCOUNT ────────────────────────────────────────────────────────
    // POST /api/v1/accounts
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        log.info("REST request to create account for: {}", request.getEmail());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)             // 201
                .body(ApiResponse.success(response, "Account created successfully"));
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    // GET /api/v1/accounts/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(
            @PathVariable UUID id) {

        log.info("REST request to get account by id: {}", id);
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET BY ACCOUNT NUMBER ─────────────────────────────────────────────────
    // GET /api/v1/accounts/number/{accountNumber}
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getByAccountNumber(
            @PathVariable String accountNumber) {

        AccountResponse response = accountService
                .getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────
    // GET /api/v1/accounts
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {

        log.info("REST request to get all accounts");
        List<AccountResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(
                ApiResponse.success(accounts,
                        accounts.size() + " accounts found")
        );
    }

    // ── GET BY STATUS ─────────────────────────────────────────────────────────
    // GET /api/v1/accounts/status/ACTIVE
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getByStatus(
            @PathVariable Account.AccountStatus status) {

        List<AccountResponse> accounts = accountService.getAccountsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    // ── DEPOSIT ───────────────────────────────────────────────────────────────
    // POST /api/v1/accounts/{id}/deposit
    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<AccountResponse>> deposit(
            @PathVariable UUID id,
            @Valid @RequestBody DepositWithdrawRequest request) {

        log.info("REST request to deposit ₹{} to account: {}",
                request.getAmount(), id);
        AccountResponse response = accountService.deposit(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Deposit successful")
        );
    }

    // ── WITHDRAW ──────────────────────────────────────────────────────────────
    // POST /api/v1/accounts/{id}/withdraw
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<AccountResponse>> withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody DepositWithdrawRequest request) {

        log.info("REST request to withdraw ₹{} from account: {}",
                request.getAmount(), id);
        AccountResponse response = accountService.withdraw(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Withdrawal successful")
        );
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────
    // PATCH /api/v1/accounts/{id}/status?status=SUSPENDED
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam Account.AccountStatus status) {

        AccountResponse response = accountService.updateAccountStatus(id, status);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Status updated successfully")
        );
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────────
    // DELETE /api/v1/accounts/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable UUID id) {

        log.info("REST request to close account: {}", id);
        accountService.deleteAccount(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Account closed successfully")
        );
    }
}