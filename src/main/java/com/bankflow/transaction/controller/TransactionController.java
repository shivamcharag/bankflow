package com.bankflow.transaction.controller;

import com.bankflow.transaction.dto.response.ApiResponse;
import com.bankflow.transaction.dto.response.TransactionResponse;
import com.bankflow.transaction.entity.Transaction;
import com.bankflow.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // ── GET ALL ───────────────────────────────────────────────────────────────
    // GET /api/v1/transactions
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions() {
        log.info("REST request to get all transactions");
        List<TransactionResponse> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(
                ApiResponse.success(transactions, transactions.size() + " transactions found")
        );
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    // GET /api/v1/transactions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable Long id) {
        log.info("REST request to get transaction by id: {}", id);
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET BY ACCOUNT ID (required endpoint) ─────────────────────────────────
    // GET /api/v1/transactions/account/{accountId}
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByAccountId(
            @PathVariable UUID accountId) {
        log.info("REST request to get transactions for accountId: {}", accountId);
        List<TransactionResponse> transactions =
                transactionService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(
                ApiResponse.success(transactions, transactions.size() + " transactions found")
        );
    }

    // ── GET BY ACCOUNT NUMBER ─────────────────────────────────────────────────
    // GET /api/v1/transactions/account/number/{accountNumber}
    @GetMapping("/account/number/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByAccountNumber(
            @PathVariable String accountNumber) {
        log.info("REST request to get transactions for accountNumber: {}", accountNumber);
        List<TransactionResponse> transactions =
                transactionService.getTransactionsByAccountNumber(accountNumber);
        return ResponseEntity.ok(
                ApiResponse.success(transactions, transactions.size() + " transactions found")
        );
    }

    // ── GET BY TYPE ───────────────────────────────────────────────────────────
    // GET /api/v1/transactions/type/DEPOSIT
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByType(
            @PathVariable Transaction.TransactionType type) {
        log.info("REST request to get transactions of type: {}", type);
        List<TransactionResponse> transactions =
                transactionService.getTransactionsByType(type);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
