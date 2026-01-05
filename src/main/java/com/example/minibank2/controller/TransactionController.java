package com.example.minibank2.controller;

import com.example.minibank2.dto.TransactionResponse;
import com.example.minibank2.dto.TransferRequest;
import com.example.minibank2.dto.TransferResponse;
import com.example.minibank2.entity.TransactionType;
import com.example.minibank2.service.AccountService;
import com.example.minibank2.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    // 🔹 GET /transactions → zwraca wszystkie transakcje danego konta
    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsForAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsForAccount(accountId));
    }

    // 🔹 GET - Pobranie transakcji z zakresu dat
    @GetMapping("/{accountId}/between")
    public ResponseEntity<List<TransactionResponse>> getTransactionsBetweenDates(
            @PathVariable Long accountId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(transactionService.getTransactionsBetweenDates(accountId, from, to));
    }

    // 🔹 GET - Pobranie tylko transakcji typu DEPOSIT / WITHDRAW
    @GetMapping("/{accountId}/type")
    public ResponseEntity<List<TransactionResponse>> getTransactionsType(
            @PathVariable Long accountId,
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionService.getTransactionsForAccountByType(accountId, type));
    }

    // 🔹 GET - Pobranie sumy transakcji z danego dnia
    @GetMapping("/{accountId}/transactions/sum")
    public ResponseEntity<BigDecimal> getTransactionSumForDate(
            @PathVariable Long accountId,
            @RequestParam LocalDateTime date) {
        return ResponseEntity.ok(transactionService.getTransactionSumForDate(accountId, date));
    }

    // 🔹 GET - Pobranie liczby transakcji na koncie
    @GetMapping("/{accountId}/count")
    public ResponseEntity<Long> getTransactionCount(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionCount(accountId));
    }

    // 🔹 GET - Pobranie ostatnich N transakcji
    @GetMapping("/{accountId}/last")
    public ResponseEntity<List<TransactionResponse>> getNTransactions(
            @PathVariable Long accountId,
            @RequestParam Integer limit) {
        return ResponseEntity.ok(transactionService.getLastNTransactions(accountId, limit));
    }

    // 🔹 GET - Pobranie największej transakcji (deposit/withdraw)
    @GetMapping("/{accountId}/max")
    public ResponseEntity<TransactionResponse> getMaxTransactionsByType(
            @PathVariable Long accountId,
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionService.getMaxTransactionsByType(accountId, type));
    }

    // 🔹 GET - Pobranie transakcji powyżej określonej kwoty
    @GetMapping("/{accountId}/above")
    public ResponseEntity<List<TransactionResponse>> getTransactionsAboveAmount(
            @PathVariable Long accountId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionService.getTransactionsAboveAmount(accountId, amount));
    }

    // 🔹 POST - wykonanie transakcji przy pomocy klasy DTO
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        accountService.transfer(request.getSenderId(), request.getReceiverId(), request.getAmount());
        return ResponseEntity.ok(new TransferResponse("Transfer completed"));
    }
}
