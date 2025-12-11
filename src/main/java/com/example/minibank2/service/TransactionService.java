package com.example.minibank2.service;

import com.example.minibank2.entity.Account;
import com.example.minibank2.entity.Transaction;
import com.example.minibank2.entity.TransactionType;
import com.example.minibank2.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // metoda pomocnicza do tworzenia obiektu Transaction
    private Transaction createTransaction(Account account, BigDecimal amount, TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDateTime(LocalDateTime.now());
        return transaction;
    }

    // metoda do zapisu transakcji wpłaty
    public void recordDeposit(Account account, BigDecimal amount) {
        Transaction transaction = createTransaction(account, amount, TransactionType.DEPOSIT);
        transactionRepository.save(transaction);
    }

    // metoda do zapisu transakcji wypłaty
    public void recordWithdraw(Account account, BigDecimal amount) {
        Transaction transaction = createTransaction(account, amount, TransactionType.WITHDRAW);
        transactionRepository.save(transaction);
    }

    // zapisywanie transferu (withdraw + deposit)
    public void recordTransfer(Account sender, Account receiver, BigDecimal amount) {
        // transakcja wychodząca z konta nadawcy
        Transaction out = createTransaction(sender, amount, TransactionType.TRANSFER_OUT);
        transactionRepository.save(out);

        // transakcja przychodząca na konto odbiorcy
        Transaction in = createTransaction(receiver, amount, TransactionType.TRANSFER_IN);
        transactionRepository.save(in);
    }

    // pobieranie historii transakcji dla konta
    public List<Transaction> getTransactionsForAccount(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByDateTimeDesc(accountId);
        if (transactions.isEmpty()) {
            throw new NoSuchElementException("No transactions for account id " + accountId);
        }
        return transactions;
    }

    // Pobranie historii filtrowanej po typie
    public List<Transaction> getTransactionsForAccountByType(Long accountId, TransactionType type) {
        return transactionRepository.findByAccountIdAndType(accountId, type);
    }

    // Pobranie transakcji z zakresu dat
    public List<Transaction> getTransactionsBetweenDates(Long accountId, LocalDateTime from, LocalDateTime to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' date cannot be before 'from' date");
        }
        return transactionRepository.findByAccountIdAndDateTimeBetween(accountId, from, to);
    }

    // Pobranie sumy transakcji z danego dnia
    public BigDecimal getTransactionSumForDate(Long accountId, LocalDateTime date) {
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateTime(accountId, date);
        return transactions.stream()
                .map(Transaction::getAmount).
                reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Pobranie liczby transakcji na koncie
    public Long getTransactionCount(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        return (long) transactions.size();
    }

    // Pobranie ostatnich N transakcji
    public List<Transaction> getLastNTransactions(Long accountId, Integer limit) {
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByDateTimeDesc(accountId);
        return transactions.stream().limit(limit).toList();
    }

    // Pobranie największej transakcji (deposit/withdraw)
    public Transaction getMaxTransactionsByType(Long accountId, TransactionType type) {
        Transaction transaction = transactionRepository.findByAccountIdAndType(accountId, type).stream()
                .max(Comparator.comparing(Transaction::getAmount)).orElse(null);
        return transaction;
    }

    // Pobranie transakcji powyżej określonej kwoty
    public List<Transaction> getTransactionsAboveAmount(Long accountId, BigDecimal amount) {
        return transactionRepository.findByAccountIdAndAmountGreaterThan(accountId, amount);
    }
}
