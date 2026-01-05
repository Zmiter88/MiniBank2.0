package com.example.minibank2.entity;

import com.example.minibank2.exception.InsufficientFundsException;
import com.example.minibank2.exception.InvalidAmountException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Klasa Account reprezentuje konto bankowe w naszej aplikacji MiniBank2.0.
 * Jest to encja JPA, co oznacza, że każda instancja tej klasy będzie odwzorowana
 * na wiersz w tabeli "accounts" w bazie danych H2.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // unikalny identyfikator konta, generowany automatycznie
    private LocalDate createdAt;
    private String owner;    // właściciel konta, np. "Alicja Kowalska"
    private String number;   // numer konta, np. "PL1234567890"
    private String currency; // waluta konta, np. "PLN"
    private BigDecimal balance; // saldo konta
    private String status;      // status konta, np. "ACTIVE" lub "BLOCKED"

    // nowe pola do testowania dodatkowych funkcji
    @Enumerated(EnumType.STRING)
    private AccountType accountType;     // typ konta, np. "SAVINGS" lub "CHECKING"

    private BigDecimal interestRate; // oprocentowanie konta

    // Domyślny konstruktor wymagany przez JPA
    public Account() {}

    // Konstruktor pełny
    public Account(String owner, String number, String currency, BigDecimal balance,
                   String status, AccountType accountType, BigDecimal interestRate) {
        this.owner = owner;
        this.number = number;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.accountType = accountType;
        this.interestRate = interestRate;
        this.createdAt = LocalDate.now();
    }

    // Gettery i settery dla wszystkich pól
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    // metody wpłaty i wypłaty środków z konta wraz z walidacją
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Kwota musi być większa od 0");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Brak wystarczających środków na koncie");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Kwota musi być większa od 0");
        }
        this.balance = this.balance.add(amount);
    }

}

