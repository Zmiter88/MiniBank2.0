package com.example.minibank2.integration;

import com.example.minibank2.entity.Account;
import com.example.minibank2.entity.AccountType;
import com.example.minibank2.entity.Transaction;
import com.example.minibank2.entity.TransactionType;
import com.example.minibank2.repository.AccountRepository;
import com.example.minibank2.repository.TransactionRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.sessionId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")

public class TransactionControllerTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    // Metoda pomocnicza do tworzenia kont dla pelnego konstruktora
    private Account createAccount(String owner, BigDecimal balance, AccountType accountType,
                                  String currency, BigDecimal interestRate, LocalDate createdAt) {
        Account account = new Account(owner, "AC" + System.currentTimeMillis(), currency, balance,
                "ACTIVE", accountType, interestRate);
        account.setCreatedAt(createdAt); // możesz ustawić różne daty
        return account;
    }

    // Metoda pomocnicza do tworzenia transakcji
    private Transaction createTransaction(
            Account account,
            TransactionType type,
            BigDecimal amount,
            LocalDateTime dateTime
    ) {
        return new Transaction(
                null,   // id wygeneruje sie automatycznie
                dateTime,
                amount,
                type,
                account
        );
    }

    // Metoda pomocnicza do tworzenia kilku przykładowych transakcji dla testów
    private List<Transaction> createTestTransactions(Account account) {
        Transaction t1 = createTransaction(account, TransactionType.DEPOSIT, BigDecimal.valueOf(1000), LocalDateTime.of(2024, 3, 3, 15, 30));
        Transaction t2 = createTransaction(account, TransactionType.WITHDRAW, BigDecimal.valueOf(300), LocalDateTime.of(2023, 3, 2, 12, 0));
        Transaction t3 = createTransaction(account, TransactionType.DEPOSIT, BigDecimal.valueOf(2000), LocalDateTime.of(2021, 3, 1, 10, 0));
        Transaction t4 = createTransaction(account, TransactionType.WITHDRAW, BigDecimal.valueOf(500), LocalDateTime.of(2020, 3, 1, 10, 0));
        return List.of(t1, t2, t3, t4);
    }


    // Test dla endpointu do zwracania wszystkich transakcji
    @Test
    void shouldReturnAllTransactionForAccount() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);

        given()
                .when()
                .get("/transactions/{accountId}", savedAccount.getId())
                .then()
                .statusCode(200)
                .body("", hasSize(transactions.size()))
                .body("[0].amount", equalTo(transactions.get(0).getAmount().floatValue()))
                .body("[0].type", equalTo(transactions.get(0).getType().name()))
                .body("[1].amount", equalTo(transactions.get(1).getAmount().floatValue()))
                .body("[1].type", equalTo(transactions.get(1).getType().name()))
                .body("[2].amount", equalTo(transactions.get(2).getAmount().floatValue()))
                .body("[2].type", equalTo(transactions.get(2).getType().name()))
                .body("[3].amount", equalTo(transactions.get(3).getAmount().floatValue()))
                .body("[3].type", equalTo(transactions.get(3).getType().name()));
    }

    @Test
    void shouldReturnDepositAccounts() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);
        TransactionType transactionType = TransactionType.DEPOSIT;
        long expectedDepositAccounts = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .count();

        given()
                .queryParam("type", transactionType.name())
                .when()
                .get("/transactions/{accountId}/type", savedAccount.getId())
                .then()
                .statusCode(200)
                .body("", hasSize((int) expectedDepositAccounts))
                .body("amount", containsInAnyOrder(1000F, 2000F));
    }

    // zwraca transakcje pomiedzy datami
    @Test
    void shouldReturnAccountsBetweenDates() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);
        LocalDateTime from = LocalDateTime.of(2022, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 11, 10, 23, 59);
        long expectedCount = transactions.stream()
                .filter(t -> (t.getDateTime().isAfter(from) || t.getDateTime().isEqual(from)) && (t.getDateTime().isBefore(to) || t.getDateTime().isEqual(to)))
                .count();

        given()
                .queryParam("from", from.toString())
                .queryParam("to", to.toString())
                .when()
                .get("/transactions/{accountId}/between", savedAccount.getId())
                .then()
                .statusCode(200)
                .body("", hasSize((int) expectedCount));
    }

    // zwraca transakcje pomiedzy datami - bad request from > to
    @Test
    void shouldReturnAccountsBetweenDatesBadRequestFromIsGreaterThanTo() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);
        LocalDateTime from = LocalDateTime.of(2024, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2022, 11, 10, 23, 59);

        given()
                .queryParam("from", from.toString())
                .queryParam("to", to.toString())
                .when()
                .get("/transactions/{accountId}/between", savedAccount.getId())
                .then()
                .statusCode(400)
                .body("message", containsString("'to' date cannot be before 'from' date"));
    }

    @Test
    void shouldReturnNotFoundWhenNoTransactionsInDateRange() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        LocalDateTime from = LocalDateTime.of(2022, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 11, 10, 23, 59);

        given()
                .queryParam("from", from.toString())
                .queryParam("to", to.toString())
                .when()
                .get("/transactions/{accountId}/between", savedAccount.getId())
                .then()
                .statusCode(404)
                .body("message", containsString("No transactions for account id " + savedAccount.getId() + " between dates"));
    }

    // Pobranie liczby transakcji na koncie
    @Test
    void shouldReturnNumberOfTransactions() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);

        Long transactionsCount =
                given()
                        .when()
                        .get("/transactions/{accountId}/count", savedAccount.getId())
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Long.class);

        assertThat(transactionsCount).isEqualTo((long) transactions.size());
    }

    // Pobranie sumy transakcji z danego dnia
    @Test
    void shouldReturnSumOfTransactionPerDay() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);

// Pobieramy sumę transakcji dla daty pierwszej transakcji
        LocalDateTime date = transactions.get(0).getDateTime();
        String isoDate = date.truncatedTo(ChronoUnit.SECONDS).toString();


        BigDecimal expectedSum = transactions.stream()
                .filter(t -> t.getDateTime().toLocalDate().equals(date.toLocalDate()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Wyciągamy odpowiedź jako BigDecimal
        BigDecimal sum = given()
                .queryParam("date", isoDate)
                .when()
                .get("/transactions/{accountId}/transactions/sum", savedAccount.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(BigDecimal.class); // <-- REST-assured odczytuje pojedynczą liczbę

        assertThat(sum).isEqualByComparingTo(expectedSum);
    }

    // Pobranie ostatnich N transakcji
    @Test
    void shouldReturnLastNTransactions() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);
        int limit = 2;

        List<String> dates =
        given()
                .queryParam("limit", limit)
                .when()
                .get("/transactions/{accountId}/last", savedAccount.getId())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("dateTime");

        assertThat(dates).hasSize(limit);
        LocalDateTime first = LocalDateTime.parse(dates.get(0));
        LocalDateTime second = LocalDateTime.parse(dates.get(1));
        assertThat(first).isAfter(second);
    }

    // Pobranie największej transakcji (deposit/withdraw)
    @Test
    void shouldReturnMaxTransactionByType() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);
        TransactionType transactionType = TransactionType.DEPOSIT;
        Transaction maxAmountTransaction = transactions.stream()
                .filter(t -> t.getType().equals(transactionType))
                        .max(Comparator.comparing(Transaction::getAmount)).orElseThrow();
        BigDecimal maxAmount = maxAmountTransaction.getAmount();

        given()
                .queryParam("type", transactionType.name())
                .when()
                .get("/transactions/{accountId}/max", savedAccount.getId())
                .then()
                .statusCode(200)
                .body("amount", equalTo(maxAmount.floatValue()))
                .body("type", equalTo(transactionType.name()));
    }

    @Test
    void shouldReturn404StatusCodeWhenNoTransaction() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        TransactionType transactionType = TransactionType.DEPOSIT;

        given()
                .queryParam("type", transactionType)
                .when()
                .get("/transactions/{accountId}/max", savedAccount.getId())
                .then()
                .statusCode(404)
                .body("message", containsString("No transactions of type " + transactionType.name() + " for account id " + savedAccount.getId()));
    }

    // Pobranie transakcji powyżej określonej kwoty
    @Test
    void shouldReturnTransactionAboveAmount() {
        // Tworzymy konto testowe
        Account account = createAccount(
                "Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)
        );
        Account savedAccount = accountRepository.save(account);
        //Tworzymy przykładowe transakcje i zapisujemy je w repo
        List<Transaction> transactions = createTestTransactions(savedAccount);
        transactionRepository.saveAll(transactions);
        BigDecimal amount = BigDecimal.valueOf(1000);
        List<Transaction> transactionsAboveAmount = transactions.stream()
                        .filter(t -> t.getAmount().compareTo(amount) > 0)
                                .toList();
        given()
                .queryParam("amount", amount)
                .when()
                .get("/transactions/{accountId}/above", savedAccount.getId())
                .then()
                .statusCode(200)
                .body("", hasSize(transactionsAboveAmount.size()))
                .body("amount", everyItem(greaterThan(amount.floatValue())));
    }
}
