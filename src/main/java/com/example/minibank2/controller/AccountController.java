package com.example.minibank2.controller;

import com.example.minibank2.model.Account;
import com.example.minibank2.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * AccountController obsługuje REST API dla kont bankowych.
 * Przyjmuje żądania HTTP i wywołuje odpowiednie metody serwisu.
 */
@RestController
@RequestMapping("/accounts") // ścieżka bazowa dla wszystkich endpointów
public class AccountController {

    private final AccountService accountService;

    // Konstruktor z wstrzykiwaniem serwisu
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Działamy na klasie ResponseEntity, ponieważ pozwala zwrócić zarówno dane, jak i dodatkowe informacje HTTP do klienta.

    // GET /accounts → zwraca wszystkie konta
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> allAccounts = accountService.getAllAccounts();
        return ResponseEntity.ok(allAccounts);
    }

    // POST /accounts → tworzy nowe konto
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        Account createdAccount = accountService.createAccount(account);
        return ResponseEntity.ok(createdAccount); // 200 OK + utworzone konto
    }

    // Metoda PUT aktualizujaca konto
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account updateAccount) {
        try {
            Account updated = accountService.updateAccount(id, updateAccount);
            return ResponseEntity.ok(updated);  // 200 OK + zaktualizowane konto
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();  // 404 Not Found
        }
    }

    // Metoda DELETE do usuwania konta zwracajaca obiekt
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.ok("Account with id " + id + " deleted successfully");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Account with id " + id + " not found");
        }
    }

    // Metoda GET do szukania konta po id
    @GetMapping("/{id}")
    public ResponseEntity<Account> findAccountById(@PathVariable Long id) {
        try {
            Account foundAccount = accountService.findAccountById(id);
            return ResponseEntity.ok(foundAccount);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Metdoa GET do pobrania konta po właścicielu
    @GetMapping("/owner/{owner}")
    public ResponseEntity<List<Account>> findAccountByOwner(@PathVariable String owner) {
        try {
            List<Account> foundAccounts = accountService.findAccountsByOwner(owner);
            return ResponseEntity.ok(foundAccounts);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Metoda GET do pobrania konta z najwyższym saldem
    @GetMapping("/highest-balance")
    public ResponseEntity<Account> getAccountWithMaxBalance() {
        try {
            Account accountWithMaxBalance = accountService.getAccountWithMaxBalanceSpring();
            return ResponseEntity.ok(accountWithMaxBalance);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Metoda GET do znajdywania kont o saldzie mniejszym niż podanym z palca
    @GetMapping("/balance/greater-than/{amount}")
    public ResponseEntity<List<Account>> getAccountsWithBalanceGreaterThan(@PathVariable BigDecimal amount) {
        List<Account> foundAccounts = accountService.getAccountsWithBalanceGreaterThan(amount);
        return foundAccounts.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(foundAccounts);
    }

    // Metoda GET do znalezienie kont utworzonych po dacie
    @GetMapping("/created-after/{date}")
    public ResponseEntity<List<Account>> getAccountsCreatedAfter(@PathVariable LocalDate date) {
        List<Account> foundAccounts = accountService.getAccountsCreatedAfterDate(date);
        return foundAccounts.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(foundAccounts);
    }

    // Metoda GET do znalezienia pierwsze konto (najstarsze) po dacie utworzenia (createdAt)
    @GetMapping("/oldest")
    public ResponseEntity<Account> getTheOldestAccount() {
        try {
            Account account = accountService.getTheOldestAccount();
            return ResponseEntity.ok(account);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Metoda GET do policzenia, ile jest kont w danej walucie (currency)
    @GetMapping("/with-currency/{currency}")
    public ResponseEntity<Long> getHowManyAccountWithCurrency(@PathVariable String currency) {
        Long accounts = accountService.getHowManyAccountWithCurrency(currency);
        return ResponseEntity.ok(accounts);
    }

    // Metoda GET do znalezienia pierwszego konta, które ma status „ACTIVE”, posortowane malejąco po saldzie
    @GetMapping("/with-status/{status}")
    public ResponseEntity<Account> getFirstActiveAccountOrderByBalanceDesc(@PathVariable String status) {
        try {
            Account account = accountService.firstActiveAccountOrderByBalanceDesc(status);
            return ResponseEntity.ok(account);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Metoda GET do znajdywania wszystkich kont utworzone przed datą
    @GetMapping("/created-before/{date}")
    public ResponseEntity<List<Account>> getAccountsCreatedBefore(@PathVariable LocalDate date) {
        List<Account> foundAccounts = accountService.accountsCreatedBefore(date);
        return foundAccounts.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(foundAccounts);
    }

    // Metoda GET znajdź konto o najwyższym saldzie w danej walucie
    @GetMapping("/highest-balance/{currency}")
    public ResponseEntity<Account> getAccountWithHighestBalanceIn(@PathVariable String currency) {
        try {
            Account account = accountService.accountWithHighestBalanceIn(currency);
            return ResponseEntity.ok(account);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Metoda GET do znajdywania 3 kont z najwzyśzym saldem
    @GetMapping("/balance-top3")
    public ResponseEntity<List<Account>> getTop3HighestBalanceAccounts() {
        List<Account> accounts = accountService.top3HighestBalanceAccounts();
        return accounts.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(accounts);
    }







    // Później można dodać np.:
    // - GET /accounts/{id} → konto po ID
    // - PUT /accounts/{id} → aktualizacja konta
    // - DELETE /accounts/{id} → usunięcie konta
}

