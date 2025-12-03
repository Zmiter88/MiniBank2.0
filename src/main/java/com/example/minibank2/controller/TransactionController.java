package com.example.minibank2.controller;

import com.example.minibank2.dto.TransferRequest;
import com.example.minibank2.dto.TransferResponse;
import com.example.minibank2.entity.Transaction;
import com.example.minibank2.entity.TransactionType;
import com.example.minibank2.service.AccountService;
import com.example.minibank2.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    // GET /transactions → zwraca wszystkie transakcje danego konta
    @GetMapping("/{accountId}")
    public ResponseEntity<List<Transaction>> getTransactionsForAccount(@PathVariable Long accountId) {
            List<Transaction> transactions = transactionService.getTransactionsForAccount(accountId);
            return transactions.isEmpty()
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.ok(transactions);
    }

    // GET - Pobranie transakcji z zakresu dat
    @GetMapping("/{accountId}/between")
    public ResponseEntity<List<Transaction>> getTransactionsBetweenDates(
            @PathVariable Long accountId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {

        List<Transaction> transactions = transactionService.getTransactionsBetweenDates(accountId, from, to);
        return transactions.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(transactions);
    }

    // GET - Pobranie tylko transakcji typu DEPOSIT / WITHDRAW
    @GetMapping("/{accountId}/type")
    public ResponseEntity<List<Transaction>> getTransactionsType(@PathVariable Long accountId, @RequestParam TransactionType type) {
        List<Transaction> transactions = transactionService.getTransactionsForAccountByType(accountId, type);
        return transactions.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(transactions);
    }

    // GET - Pobranie sumy transakcji z danego dnia
    @GetMapping("/{accountId}/transactions/sum")
    public ResponseEntity<BigDecimal> getTransactionSumForDate(@PathVariable Long accountId, @RequestParam LocalDateTime date) {
        BigDecimal sum = transactionService.getTransactionSumForDate(accountId, date);
        return ResponseEntity.ok(sum);
    }

    // GET - Pobranie liczby transakcji na koncie
    @GetMapping("/{accountId}/count")
    public ResponseEntity<Long> getTransactionCount(@PathVariable Long accountId) {
        Long count = transactionService.getTransactionCount(accountId);
        return ResponseEntity.ok(count);
    }

    // GET - Pobranie ostatnich N transakcji
    @GetMapping("/{accountId}/last")
    public ResponseEntity<List<Transaction>> getNTransactions(@PathVariable Long accountId, @RequestParam Integer limit) {
        List<Transaction> transactions = transactionService.getLastNTransactions(accountId, limit);
        return transactions.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(transactions);
    }

    // GET - Pobranie największej transakcji (deposit/withdraw)
    @GetMapping("/{accountId}/max")
    public ResponseEntity<Transaction> getMaxTransactionsByType(@PathVariable Long accountId, @RequestParam TransactionType type) {
        Transaction transaction = transactionService.getMaxTransactionsByType(accountId, type);
        return transaction != null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(transaction);
    }

    // GET - Pobranie transakcji powyżej określonej kwoty
    @GetMapping("/{accountId}/above")
    public ResponseEntity<List<Transaction>> getTransactionsAboveAmount(@PathVariable Long accountId, @RequestParam BigDecimal amount) {
        List<Transaction> transactions = transactionService.getTransactionsAboveAmount(accountId, amount);
        return transactions.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(transactions);
    }

    // POST - wykonannie transakcji przy pomocy klasy DTO
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        accountService.transfer(request.getSenderId(), request.getReceiverId(), request.getAmount());
        return ResponseEntity.ok(new TransferResponse("Transfer completed"));
    }
}
