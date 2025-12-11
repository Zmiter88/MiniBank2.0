package com.example.minibank2.integration;

import com.example.minibank2.entity.Account;
import com.example.minibank2.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountRepositoryIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void testFindByOwner() {
        List<Account> accounts = accountRepository.findByOwner("Alicja Kowalska");
        assertThat(accounts).isNotEmpty(); // sprawdza, że dane z DataInitializer są w bazie
        assertThat(accounts.get(0).getBalance()).isEqualTo("5200.50"); // dodatkowa kontrola
    }

    @Test
    void testFindTopByOrderByBalanceDesc() {
        Optional<Account> account = accountRepository.findTopByOrderByBalanceDesc();
        assertThat(account).isPresent();
        assertThat(account.get().getOwner()).isEqualTo("Maria Wiśniewska");
    }

    @Test
    void testFindByBalanceGreaterThan() {
        BigDecimal threshold = new BigDecimal("5000");
        List<Account> accounts = accountRepository.findByBalanceGreaterThan(threshold);
        assertThat(accounts).isNotEmpty();
        assertThat(accounts).extracting(Account::getOwner).containsExactlyInAnyOrder("Alicja Kowalska", "Maria Wiśniewska");
    }

    @Test
    void testFindByCreatedAtAfter() {
        LocalDate date = LocalDate.parse("2025-03-21");
        List<Account> accounts = accountRepository.findByCreatedAtAfter(date);
        assertThat(accounts).isNotEmpty();
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getOwner).containsExactlyInAnyOrder("Tomasz Zieliński", "Barbara Sawicka");
    }

    @Test
    void testFindTopByOrderByCreatedAtAsc() {
        Optional<Account> account = accountRepository.findTopByOrderByCreatedAtAsc();
        assertThat(account).isPresent();
        assertThat(account.get().getOwner()).isEqualTo("Alicja Kowalska");
    }

    @Test
    void testCountByCurrency() {
        String currency = "PLN";
        Long quantity = accountRepository.countByCurrency(currency);
        assertThat(quantity).isEqualTo(3);
    }

    @Test
    void testFindTopByStatusOrderByBalanceDesc() {
        String status = "ACTIVE";
        Optional<Account> account = accountRepository.findTopByStatusOrderByBalanceDesc(status);
        assertThat(account).isPresent();
        assertThat(account.get().getOwner()).isEqualTo("Maria Wiśniewska");
    }

    @Test
    void testFindAllByCreatedAtBefore() {
        LocalDate date = LocalDate.parse("2025-04-12");
        List<Account> accounts = accountRepository.findAllByCreatedAtBefore(date);
        assertThat(accounts).isNotEmpty();
        assertThat(accounts).hasSize(4);
        assertThat(accounts).extracting(Account::getOwner).containsExactlyInAnyOrder("Maria Wiśniewska", "Alicja Kowalska", "Jan Nowak", "Tomasz Zieliński");
    }
    // albo wersja dynamiczna tego samego tetsu, czyli, sprawdzenie tylko, że wszystkie zwrócone konta mają createdAt przed podaną datą, bez podawania dokładnej listy

    @Test
    void testFindAllByCreatedAtBeforeDynamic() {
        LocalDate date = LocalDate.parse("2025-04-14");
        List<Account> accounts = accountRepository.findAllByCreatedAtBefore(date);
        assertThat(accounts).isNotEmpty();
        assertThat(accounts).allMatch(account -> account.getCreatedAt().isBefore(date));
    }

    @Test
    void testFindTopByCurrencyOrderByBalanceDesc() {
        String currency = "PLN";
        Optional<Account> account = accountRepository.findTopByCurrencyOrderByBalanceDesc(currency);
        assertThat(account).isPresent();
        assertThat(account.get().getOwner()).isEqualTo("Alicja Kowalska");
    }

    @Test
    void testFindTop3ByOrderByBalanceDesc() {
        List<Account> accounts = accountRepository.findTop3ByOrderByBalanceDesc();
        assertThat(accounts).isNotEmpty();
        assertThat(accounts).hasSize(3);
        assertThat(accounts).extracting(Account::getOwner).containsExactlyInAnyOrder("Alicja Kowalska", "Maria Wiśniewska", "Barbara Sawicka");
    }


}
