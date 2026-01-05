package com.example.minibank2.controller;

import com.example.minibank2.dto.*;
import com.example.minibank2.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * AccountController obsługuje REST API dla kont bankowych.
 * Przyjmuje żądania HTTP i wywołuje odpowiednie metody serwisu.
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    // Konstruktor z wstrzykiwaniem serwisu
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // 🔹 GET /accounts → zwraca wszystkie konta
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> foundAccounts = accountService.getAllAccounts();
        return foundAccounts.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(foundAccounts);
    }

    // 🔹 POST /accounts → tworzy nowe konto
    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(@RequestBody @Valid CreateAccountRequest request) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    // 🔹 PUT /accounts/{id} → aktualizacja konta
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id, @RequestBody @Valid UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    // 🔹 DELETE /accounts/{id} → usunięcie konta
    @DeleteMapping("/{id}")
    public ResponseEntity<AccountResponse> deleteAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.deleteAccount(id));
    }

    // 🔹 GET /accounts/{id} → pobranie konta po id
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.findAccountById(id));
    }

    // 🔹 GET /accounts/owner/{owner} → pobranie kont po właścicielu
    @GetMapping("/owner/{owner}")
    public ResponseEntity<List<AccountResponse>> findAccountByOwner(@PathVariable String owner) {
        return ResponseEntity.ok(accountService.findAccountsByOwner(owner));
    }

    // 🔹 GET /accounts/highest-balance → konto z najwyższym saldem
    @GetMapping("/highest-balance")
    public ResponseEntity<AccountResponse> getAccountWithMaxBalance() {
        return ResponseEntity.ok(accountService.getAccountWithMaxBalanceSpring());
    }

    // 🔹 GET /accounts/balance/greater-than/{amount} → konta z saldem większym niż podane
    @GetMapping("/balance/greater-than/{amount}")
    public ResponseEntity<List<AccountResponse>> getAccountsWithBalanceGreaterThan(@PathVariable BigDecimal amount) {
        List<AccountResponse> accounts = accountService.getAccountsWithBalanceGreaterThan(amount);
        return accounts.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(accounts);
    }

    // 🔹 GET /accounts/created-after/{date} → konta utworzone po dacie
    @GetMapping("/created-after/{date}")
    public ResponseEntity<List<AccountResponse>> getAccountsCreatedAfter(@PathVariable LocalDate date) {
        List<AccountResponse> accounts = accountService.getAccountsCreatedAfterDate(date);
        return accounts.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(accounts);
    }

    // 🔹 GET /accounts/oldest → najstarsze konto
    @GetMapping("/oldest")
    public ResponseEntity<AccountResponse> getTheOldestAccount() {
        return ResponseEntity.ok(accountService.getTheOldestAccount());
    }

    // 🔹 GET /accounts/with-currency/{currency} → liczba kont w danej walucie
    @GetMapping("/with-currency/{currency}")
    public ResponseEntity<Long> getHowManyAccountWithCurrency(@PathVariable String currency) {
        return ResponseEntity.ok(accountService.getHowManyAccountWithCurrency(currency));
    }

    // 🔹 GET /accounts/with-status/{status} → pierwsze aktywne konto według salda
    @GetMapping("/with-status/{status}")
    public ResponseEntity<AccountResponse> getFirstActiveAccountOrderByBalanceDesc(@PathVariable String status) {
        return ResponseEntity.ok(accountService.firstActiveAccountOrderByBalanceDesc(status));
    }

    // 🔹 GET /accounts/created-before/{date} → konta utworzone przed datą
    @GetMapping("/created-before/{date}")
    public ResponseEntity<List<AccountResponse>> getAccountsCreatedBefore(@PathVariable LocalDate date) {
        List<AccountResponse> accounts = accountService.accountsCreatedBefore(date);
        return accounts.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(accounts);
    }

    // 🔹 GET /accounts/highest-balance/{currency} → konto z najwyższym saldem w danej walucie
    @GetMapping("/highest-balance/{currency}")
    public ResponseEntity<AccountResponse> getAccountWithHighestBalanceIn(@PathVariable String currency) {
        return ResponseEntity.ok(accountService.accountWithHighestBalanceIn(currency));
    }

    // 🔹 GET /accounts/balance-top3 → top 3 kont z najwyższym saldem
    @GetMapping("/balance-top3")
    public ResponseEntity<List<AccountResponse>> getTop3HighestBalanceAccounts() {
        List<AccountResponse> accounts = accountService.top3HighestBalanceAccounts();
        return accounts.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(accounts);
    }

    // 🔹 POST /accounts/transfer → wykonanie przelewu
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody @Valid TransferRequest request) {
        accountService.transfer(request.getSenderId(), request.getReceiverId(), request.getAmount());
        return ResponseEntity.ok("Transfer completed");
    }

    // POST /accounts/{id}/deposit
    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountService.deposit(id, amount));
    }

    // POST /accounts/{id}/withdraw
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountService.withdraw(id, amount));
    }

}
