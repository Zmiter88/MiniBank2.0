package com.example.minibank2.service;

import com.example.minibank2.model.Account;
import com.example.minibank2.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * AccountService to warstwa logiki biznesowej dla kont bankowych.
 * Odpowiada za pobieranie danych z repozytorium i wykonywanie operacji na kontach.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);


    // Konstruktor z wstrzykiwaniem repozytorium (Dependency Injection)
    public AccountService(AccountRepository accountRepository) {

        this.accountRepository = accountRepository;
    }

    // Zwraca listę wszystkich kont
    public List<Account> getAllAccounts() {

        return accountRepository.findAll();
    }

    // Tworzy nowe konto i zapisuje je w bazie
    public Account createAccount(Account account) {

        return accountRepository.save(account);
    }

    // Metoda do aktualizowania konta (tylko wybrane pola, te które użytkownik w realnym świecie może sam zmienić)
    public Account updateAccount(Long id, Account updateAccount) {
        // szukamy konta o podanym id w bazie
        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Account not found with id " + id));
        // aktualizujemy tylko wybrane pola
        existingAccount.setOwner(updateAccount.getOwner());
        existingAccount.setBalance(updateAccount.getBalance());
        existingAccount.setStatus(updateAccount.getStatus());

        // zapisujemy zmiany do bazy
        return accountRepository.save(existingAccount);
    }

    // Metoda do usuwania konta
    public Account deleteAccount(Long id) {
        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Account not found with id " + id));
        accountRepository.delete(existingAccount);
        logger.info("Account with id {} has been deleted", id);
        return existingAccount;
    }

    // Metoda do szukania konta po id
    public Account findAccountById(Long id) {
        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Account not found with id " + id));
        return existingAccount;
    }

    // Metoda do pobierania kont na podstawie właściciela
    public List<Account> findAccountsByOwner(String owner) {
        List<Account> accountsByOwner = accountRepository.findByOwner(owner);
        return accountsByOwner;
    }

    // Metoda do pobrania konta z najwyższym saldem przy pomocy stream
    public Account getAccountWithMaxBalanceStream() {
        return accountRepository.findAll().stream()
                .max(Comparator.comparing(Account::getBalance))
                .orElseThrow(() -> new NoSuchElementException("List of accounts is empty"));
    }
    // Metoda do pobrania konta z najwyższym saldem przy pomocy Spring
    public Account getAccountWithMaxBalanceSpring() {
        return accountRepository.findTopByOrderByBalanceDesc()
                .orElseThrow(() -> new NoSuchElementException("No accounts in database"));
    }

    // Metoda do znajdywania konta o saldzie wiekszym niż podanym z palca
    public List<Account> getAccountsWithBalanceGreaterThan(BigDecimal amount) {
        return accountRepository.findByBalanceGreaterThan(amount);
    }

    // Metoda do znalezienie kont utworzonych po dacie
    public List<Account> getAccountsCreatedAfterDate(LocalDate date) {
        return accountRepository.findByCreatedAtAfter(date);
    }

    // Metoda do znalezienia pierwsze konto (najstarsze) po dacie utworzenia (createdAt)
    public Account getTheOldestAccount() {
        return accountRepository.findTopByOrderByCreatedAtAsc()
                .orElseThrow(() -> new NoSuchElementException("No accounts found"));
    }

    // Metoda do policzneia, ile jest kont w danej walucie (currency)
    public Long getHowManyAccountWithCurrency(String currency) {
        return accountRepository.countByCurrency(currency);
    }

    // Metoda do znalezienia pierwszego konta, które ma status „ACTIVE”, posortowane malejąco po saldzie
    public Account firstActiveAccountOrderByBalanceDesc(String status) {
        return accountRepository.findTopByStatusOrderByBalanceDesc(status)
                .orElseThrow(() -> new NoSuchElementException("No account found with status " + status));
    }

    // Metoda do znajdywania wszystkich kont utworzone przed
    public List<Account> accountsCreatedBefore(LocalDate date) {
        return accountRepository.findAllByCreatedAtBefore(date);
    }

    // Metoda znajdź konto o najwyższym saldzie w danej walucie
    public Account accountWithHighestBalanceIn(String currency) {
        return accountRepository.findTopByCurrencyOrderByBalanceDesc(currency)
                .orElseThrow(() -> new NoSuchElementException("No account found"));
    }

    // Metoda do znajdywania 3 kont z najwzyśzym saldem
    public List<Account> top3HighestBalanceAccounts() {
        return accountRepository.findTop3ByOrderByBalanceDesc();
    }

}

