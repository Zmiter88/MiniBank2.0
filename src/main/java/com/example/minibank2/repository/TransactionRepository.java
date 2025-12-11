package com.example.minibank2.repository;

import com.example.minibank2.entity.Transaction;
import com.example.minibank2.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // wszystkie transakcje dla danego konta
    List<Transaction> findByAccountId(Long accountId);

    // transakcje dla danego konta posortowane po dacie
    List<Transaction> findByAccountIdOrderByDateTimeDesc(Long accountId);

    // Pobranie transakcji dla konta + typ transakcji
    List<Transaction> findByAccountIdAndType(Long accountId, TransactionType type);

    // Pobranie transakcji z zakresu dat
    List<Transaction> findByAccountIdAndDateTimeBetween(Long accountId, LocalDateTime from, LocalDateTime to);

    // Pobranie transakcji z danego dnia
    List<Transaction> findByAccountIdAndDateTime(Long accountId, LocalDateTime date);

    // Pobranie transakcji powyżej określonej kwoty
    List<Transaction> findByAccountIdAndAmountGreaterThan(Long accountId, BigDecimal amount);

}
