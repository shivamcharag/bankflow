package com.bankflow.transaction.service;

import com.bankflow.transaction.dto.response.TransactionResponse;
import com.bankflow.transaction.entity.Transaction;
import com.bankflow.transaction.event.TransactionEvent;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse saveTransaction(TransactionEvent event);

    TransactionResponse getTransactionById(Long id);

    List<TransactionResponse> getTransactionsByAccountId(UUID accountId);

    List<TransactionResponse> getTransactionsByAccountNumber(String accountNumber);

    List<TransactionResponse> getAllTransactions();

    List<TransactionResponse> getTransactionsByType(Transaction.TransactionType type);
}
