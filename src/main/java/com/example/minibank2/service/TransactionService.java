package com.example.minibank2.service;

import com.example.minibank2.dto.TransactionResponse;
import com.example.minibank2.entity.Account;
import com.example.minibank2.entity.Transaction;
import com.example.minibank2.entity.TransactionType;
import com.example.minibank2.exception.TransactionNotFoundException;
import com.example.minibank2.mapper.TransactionMapper;
import com.example.minibank2.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public TransactionService(TransactionRepository transactionRepository, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    // 🔹 Metoda pomocnicza do tworzenia obiektu Transaction
    private Transaction createTransaction(Account account, BigDecimal amount, TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDateTime(LocalDateTime.now());
        return transaction;
    }

    // 🔹 Metoda do zapisu transakcji wpłaty
    public void recordDeposit(Account account, BigDecimal amount) {
        transactionRepository.save(createTransaction(account, amount, TransactionType.DEPOSIT));
    }

    // 🔹 Metoda do zapisu transakcji wypłaty
    public void recordWithdraw(Account account, BigDecimal amount) {
        transactionRepository.save(createTransaction(account, amount, TransactionType.WITHDRAW));
    }

    // 🔹 Zapisywanie transferu (withdraw + deposit)
    public void recordTransfer(Account sender, Account receiver, BigDecimal amount) {
        transactionRepository.save(createTransaction(sender, amount, TransactionType.TRANSFER_OUT));
        transactionRepository.save(createTransaction(receiver, amount, TransactionType.TRANSFER_IN));
    }

    // 🔹 Pobieranie historii transakcji dla konta
    public List<TransactionResponse> getTransactionsForAccount(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByDateTimeDesc(accountId);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions for account id " + accountId);
        }
        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .toList();
    }

    // 🔹 Pobranie historii filtrowanej po typie
    public List<TransactionResponse> getTransactionsForAccountByType(Long accountId, TransactionType type) {
        List<Transaction> transactions = transactionRepository.findByAccountIdAndType(accountId, type);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions of type " + type + " for account id " + accountId);
        }
        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .toList();
    }

    // 🔹 Pobranie transakcji z zakresu dat
    public List<TransactionResponse> getTransactionsBetweenDates(Long accountId, LocalDateTime from, LocalDateTime to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' date cannot be before 'from' date");
        }
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateTimeBetween(accountId, from, to);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions for account id " + accountId + " between dates");
        }
        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .toList();
    }

    // 🔹 Pobranie sumy transakcji z danego dnia
    public BigDecimal getTransactionSumForDate(Long accountId, LocalDateTime date) {
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateTime(accountId, date);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions for account id " + accountId + " on date " + date);
        }
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 🔹 Pobranie liczby transakcji na koncie
    public Long getTransactionCount(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions for account id " + accountId);
        }
        return (long) transactions.size();
    }

    // 🔹 Pobranie ostatnich N transakcji
    public List<TransactionResponse> getLastNTransactions(Long accountId, Integer limit) {
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByDateTimeDesc(accountId);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions for account id " + accountId);
        }
        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .limit(limit)
                .toList();
    }

    // 🔹 Pobranie największej transakcji (deposit/withdraw)
    public TransactionResponse getMaxTransactionsByType(Long accountId, TransactionType type) {
        return transactionRepository.findByAccountIdAndType(accountId, type).stream()
                .max(Comparator.comparing(Transaction::getAmount))
                .map(transactionMapper::toTransactionResponse)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "No transactions of type " + type + " for account id " + accountId));
    }

    // 🔹 Pobranie transakcji powyżej określonej kwoty
    public List<TransactionResponse> getTransactionsAboveAmount(Long accountId, BigDecimal amount) {
        List<Transaction> transactions = transactionRepository.findByAccountIdAndAmountGreaterThan(accountId, amount);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions above amount " + amount + " for account id " + accountId);
        }
        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .toList();
    }
}
