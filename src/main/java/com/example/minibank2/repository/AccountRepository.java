package com.example.minibank2.repository;

import com.example.minibank2.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * AccountRepository to interfejs, który zarządza encją Account w bazie danych.
 * Dzięki dziedziczeniu po JpaRepository, otrzymujemy gotowe metody do:
 * - zapisywania kont (save)
 * - znajdowania kont po ID (findById)
 * - usuwania (delete)
 * - pobierania wszystkich kont (findAll)
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Możemy tu dodawać własne zapytania, np.:
    // List<Account> findByOwner(String owner);

    List<Account> findByOwner(String owner);
    Optional<Account> findTopByOrderByBalanceDesc();  // metoda do znalezienia konta z najwyższym saldem
    List<Account> findByBalanceGreaterThan(BigDecimal amount); // metoda do znajdywania kont o saldzie większym niż podanym z palca
    List<Account> findByCreatedAtAfter(LocalDate date); // znalezienie konta utworzonego po dacie
    Optional<Account> findTopByOrderByCreatedAtAsc(); // pierwsze konto (najstarsze) po dacie utworzenia (createdAt).
    Long countByCurrency(String currency); // Policz, ile jest kont w danej walucie (currency).
    Optional<Account> findTopByStatusOrderByBalanceDesc(String status); // Znajdź pierwsze konto, które ma status „ACTIVE”, posortowane malejąco po saldzie.
    List<Account> findAllByCreatedAtBefore(LocalDate date); // Znajdź wszystkie konta utworzone przed
    Optional<Account> findTopByCurrencyOrderByBalanceDesc(String currency); // Znajdź konto o najwyższym saldzie w danej walucie
    List<Account> findTop3ByOrderByBalanceDesc(); // 3 konta z najwyższym saldem
    Page<Account> findByBalanceGreaterThan(BigDecimal amount, Pageable pageable);
    Page<Account> findByCreatedAtAfter(LocalDate date, Pageable pageable);
    Page<Account> findByOwner(String owner, Pageable pageable);



}
