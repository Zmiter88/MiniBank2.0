package com.example.minibank2.integration;

import com.example.minibank2.dto.TransferRequest;
import com.example.minibank2.entity.Account;
import com.example.minibank2.entity.AccountType;
import com.example.minibank2.repository.AccountRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class AccountControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        accountRepository.deleteAll();
    }

    // Metoda pomocnicza do tworzenia kont
    private Account createAccount(String owner, BigDecimal balance, AccountType accountType) {
        Account account = new Account();
        account.setOwner(owner);
        account.setCurrency("PLN");
        account.setBalance(balance);
        account.setAccountType(accountType);
        account.setStatus("ACTIVE");
        return account;
    }

    // Metoda pomocnicza do tworzenia kont dla pelnego konstruktora
    private Account createAccount(String owner, BigDecimal balance, AccountType accountType,
                                  String currency, BigDecimal interestRate, LocalDate createdAt) {
        Account account = new Account(owner, "AC" + System.currentTimeMillis(), currency, balance,
                "ACTIVE", accountType, interestRate);
        account.setCreatedAt(createdAt); // możesz ustawić różne daty
        return account;
    }


    @Test
    void shouldReturnAccounts() {
        // dane testowe
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING)
        );

        accountRepository.saveAll(accounts);

        given()
                .when()
                .get("/accounts")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].owner", equalTo("Jan Kowalski"))
                .body("[0].currency", equalTo("PLN"))
                .body("[0].balance", equalTo(1000.0F));
    }

    @Test
    void shouldReturnAccountById() {
        Account account = new Account();
        account.setOwner("Jan Kowalski");
        account.setCurrency("PLN");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setAccountType(AccountType.CHECKING);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);
        given()
                .when()
                .get("/accounts/{id}", savedAccount.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(savedAccount.getId().intValue()))
                .body("owner", equalTo("Jan Kowalski"))
                .body("balance", equalTo(1000.0F));
    }

    // Sprawdzenie działania paginacji
    @Test
    void shouldReturnPagedAccounts() {
        // given - dane testowe
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS)
        );
        accountRepository.saveAll(accounts);
        // when = then
        given()
                .queryParam("page", 0)
                .queryParam("size", 4)
                .when()
                .get("/accounts/paged")
                .then()
                .statusCode(200)
                .body("content.size()", is(4))
                .body("pageable.pageNumber", is(0))
                .body("totalElements", is(4));
    }

    // sprawdzenie działania paginacji - 2 strona
    @Test
    void shouldReturnPagedAccountsPage2() {
        // given - dane testowe
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING)
        );
        accountRepository.saveAll(accounts);
        // when = then
        given()
                .queryParam("page", 1)
                .queryParam("size", 2)
                .when()
                .get("/accounts/paged")
                .then()
                .statusCode(200)
                .body("content.size()", is(2))
                .body("pageable.pageNumber", is(1))
                .body("totalElements", is(5));
    }

    //Sprawdzenie wystepowanie dwóch tych samych ownerów
    @Test
    void shouldReturnMoreThanOneAccountForSameOwner() {
        // given
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS)
        );
        accountRepository.saveAll(accounts);

        // when + then
        given()
                .when()
                .get("/accounts")
                .then()
                .statusCode(200)
                .body("findAll { it.owner == 'Jan Kowalski' }.size()", greaterThan(1));
    }

    // Sprawdzenie że nie znajduje konta i zwraca 404
    @Test
    void shouldReturn404WhenAccountNotFound() {
        given()
                .when()
                .get("/accounts/{id}", 9999)
                .then()
                .statusCode(404);
    }

    // Sortowanie po balance malejaco
    @Test
    void shouldReturnAccountsSortedByBalanceDesc() {
        // given - dane testowe
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING)
        );
        accountRepository.saveAll(accounts);
        List<Float> balances =
                given()
                        .queryParam("sort", "balance,desc")
                        .when()
                        .get("/accounts/paged")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("content.balance");

        assertThat(balances)
                .isSortedAccordingTo(Comparator.reverseOrder());

    }

    // filtr: konto o konkretnym ownerze
    @Test
    void shouldFilterByOwner() {
        // given - dane testowe
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS)
        );
        accountRepository.saveAll(accounts);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/accounts/owner/{owner}/paged", "Jan Kowalski")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content.owner", everyItem(equalTo("Jan Kowalski")));
    }

    // 10 zadań z chata

    // 1. Pobranie wszystkich kont - Sprawdzić, czy endpoint /accounts zwraca listę kont.

    @Test
    void shouldReturnAllAccounts() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING)
        );
        accountRepository.saveAll(accounts);

        given()
                .when()
                .get("/accounts")
                .then()
                .statusCode(200)
                .body("size()", equalTo(5))
                .body("id[0]", equalTo(1))
                .body("owner[0]", equalTo("Jan Kowalski"))
                .body("balance[1]", equalTo(2500F))
                .body("currency", everyItem(equalTo("PLN")));
    }

    // 2. Pobranie konta po ID
    @Test
    void shouldReturnAccountByID() {
        Account account = createAccount("Jan Kowalski", BigDecimal.valueOf(10000), AccountType.CHECKING);
        accountRepository.save(account);
        given()
                .when()
                .get("/accounts/{id}", account.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(account.getId().intValue()))
                .body("owner", equalTo("Jan Kowalski"))
                .body("balance", equalTo(10000F))
                .body("accountType", equalTo("CHECKING"));
    }

    // 3. Pobranie nieistniejącego konta
    @Test
    void shouldReturn404WhenNotExistingAccount() {
        given()
                .when()
                .get("/accounts/{id}", 999)
                .then()
                .statusCode(404);
    }

    // 4. Tworzenie konta
    @Test
    void shouldCreateNewAccount() {
        String requestBody = """
                {
                "owner":"Jan",
                "currency":"PLN",
                "accountType":"SAVINGS"
                }
                """;
        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/accounts")
                .then()
                .statusCode(200)
                .body("owner", equalTo("Jan"))
                .body("currency", equalTo("PLN"))
                .body("accountType", equalTo("SAVINGS"))
                .body("balance", equalTo(0))
                .body("status", equalTo("ACTIVE"));
    }

    // 5. Aktualizacja konta (MOZNA ZMIENIĆ TYLKO POLE OWNER, CZYLI RESZTA PÓL POWINNA ZOSTAĆ JAK BYŁA)
    @Test
    void shouldUpdateAccount() {
        Account account = createAccount("Jan", BigDecimal.valueOf(20), AccountType.SAVINGS);
        accountRepository.save(account);

        String updatedBody = """
                {
                "owner":"Patryk",
                "balance":10000,
                "accountType":"CHECKING"
                }
                """;
        given()
                .contentType("application/json")
                .body(updatedBody)
                .when()
                .put("/accounts/{id}", account.getId().intValue())
                .then()
                .statusCode(200)
                .body("owner", equalTo("Patryk"))
                .body("balance", equalTo(20.0F))
                .body("accountType", equalTo("SAVINGS"));
    }

    @Test
    void shouldReturn404WhenUpdatingNotExistingAccount() {
        String updatedBody = """
                {
                "owner":"Patryk",
                "balance":10000,
                "accountType":"CHECKING"
                }
                """;
        given()
                .contentType("application/json")
                .body(updatedBody)
                .when()
                .put("/accounts/{id}", 999)
                .then()
                .statusCode(404);
    }

    // 6. Usuwanie konta
    @Test
    void shouldDeleteAccount() {
        Account account = createAccount("Jan", BigDecimal.valueOf(20), AccountType.SAVINGS);
        accountRepository.save(account);
        given()
                .when()
                .delete("/accounts/{id}", account.getId().intValue())
                .then()
                .statusCode(200);
        // Sprawdzenie, że konto nie istnieje w bazie
        assertThat(accountRepository.findById(account.getId())).isEmpty();
    }

    @Test
    void shouldReturn404WhenDeleteNotExistingAccount() {
        given()
                .when()
                .delete("/accounts/{id}", 999)
                .then()
                .statusCode(404);
    }

    // 7. Paginacja kont
    @Test
    void shouldReturnAccountsPaged() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING)
        );
        accountRepository.saveAll(accounts);

        given()
                .queryParam("page", 0)
                .queryParam("size", 5)
                .when()
                .get("/accounts/paged")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5))
                .body("pageable.pageNumber", equalTo(0))
                .body("totalElements", equalTo(5));
    }

    @Test
    void shouldReturnAccountsPagedParametrized() {
        int page = 0;
        int size = 5;
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING)
        );
        accountRepository.saveAll(accounts);

        given()
                .queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get("/accounts/paged")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(size))
                .body("pageable.pageNumber", equalTo(page))
                .body("totalElements", equalTo(accounts.size()));
    }

    // 8. Sortowanie kont
    @Test
    void shouldReturnSortedAccountsByBalance() {
        String asc = "balance,asc";
        String desc = "balance,desc";
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING)
        );
        accountRepository.saveAll(accounts);

        List<Float> balances =
                given()
                        .queryParam("sort", desc)
                        .when()
                        .get("/accounts/paged")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("content.balance");
        assertThat(balances).isSortedAccordingTo(Comparator.reverseOrder());
    }

    // 9. Filtracja kont po właścicielu
    @Test
    void shouldReturnAccountByOwnerPaged() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING)
        );
        String owner = accounts.get(0).getOwner();  // Jan Kowalski
        accountRepository.saveAll(accounts);
        given()
                .when()
                .get("/accounts/owner/{owner}/paged", owner)
                .then()
                .statusCode(200)
                .body("content.owner", everyItem(equalTo(owner)))
                .body("content.size()", equalTo(2));
    }

    // 10. Wyjątki i walidacja
    @Test
    void shouldReturnValidationMessageAndStatus400WithoutOwner() {
        String requestBody = """
                {
                "currency":"PLN",
                "accountType":"SAVINGS"
                }
                """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/accounts")
                .then()
                .statusCode(400)
                .body("message", containsString("Owner cannot be blank"));
    }

    @Test
    void shouldReturnValidationMessageAndStatus400WithoutCurrency() {
        String requestBody = """
                {
                "owner":"Jan Kowalski",
                "accountType":"SAVINGS"
                }
                """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/accounts")
                .then()
                .statusCode(400)
                .body("message", containsString("Currency cannot be blank"));
    }

    @Test
    void shouldReturnValidationMessageAndStatus400WithoutAccountType() {
        String requestBody = """
                {
                "owner":"Jan Kowalski",
                "currency":"PLN"
                }
                """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/accounts")
                .then()
                .statusCode(400)
                .body("message", containsString("Account type is required"));
    }

    @Test
    void shouldReturnValidationMessageAndStatus400WithoutTwoFields() {
        String requestBody = """
                {
                "currency":"PLN"
                }
                """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/accounts")
                .then()
                .statusCode(400)
                .body("message", containsString("Account type is required"))
                .body("message", containsString("Owner cannot be blank"));
    }

    // Top 3 po balance
    @Test
    void shouldReturnTop3ByBalance() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING)
        );
        accountRepository.saveAll(accounts);

        List<Float> balances =
                given()
                        .queryParam("page", 0)
                        .queryParam("size", 4)
                        .queryParam("sort", "balance,desc")
                        .when()
                        .get("/accounts/balance-top3/paged")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("content.balance");
        assertThat(balances).isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(balances.size()).isEqualTo(3);
        assertThat(balances).containsExactly(10000f, 2500f, 2000f);
    }

    // /accounts/with-currency/{currency} → liczba kont w danej walucie
    @Test
    void shouldReturnHowManyAccountWithCurrency() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING, "EUR", BigDecimal.ZERO, LocalDate.of(2021, 3, 1)),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS, "PLN", BigDecimal.valueOf(0.02), LocalDate.of(2025, 1, 20)),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING, "USD", BigDecimal.ZERO, LocalDate.of(2025, 2, 15)),
                createAccount("Anna Nowak", BigDecimal.valueOf(7000), AccountType.SAVINGS, "EUR", BigDecimal.valueOf(0.02), LocalDate.of(2025, 3, 10))
        );

        accountRepository.saveAll(accounts);
        String currency = "PLN";
        Long accountsQuantity =
                given()
                        .when()
                        .get("/accounts/with-currency/{currency}", currency)
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Long.class);
        assertThat(accountsQuantity).isEqualTo(2);
    }

    // /accounts/oldest → najstarsze konto
    @Test
    void shouldReturnTheOldestAccount() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING, "EUR", BigDecimal.ZERO, LocalDate.of(2021, 3, 1)),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS, "PLN", BigDecimal.valueOf(0.02), LocalDate.of(2025, 1, 20)),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING, "USD", BigDecimal.ZERO, LocalDate.of(2025, 2, 15)),
                createAccount("Anna Nowak", BigDecimal.valueOf(7000), AccountType.SAVINGS, "EUR", BigDecimal.valueOf(0.02), LocalDate.of(2025, 3, 10))
        );
        accountRepository.saveAll(accounts);

        given()
                .when()
                .get("/accounts/oldest")
                .then()
                .statusCode(200)
                .body("owner", equalTo("Jan Kowalski"))
                .body("createdAt", equalTo("2020-01-10"));
    }

    // /accounts/balance/greater-than/{amount} → konta z saldem większym niż podane
    @Test
    void shouldReturnAccountWithBalanceMoreThan() {
        BigDecimal amount = BigDecimal.valueOf(2000);
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING, "EUR", BigDecimal.ZERO, LocalDate.of(2021, 3, 1)),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS, "PLN", BigDecimal.valueOf(0.02), LocalDate.of(2025, 1, 20)),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING, "USD", BigDecimal.ZERO, LocalDate.of(2025, 2, 15)),
                createAccount("Anna Nowak", BigDecimal.valueOf(7000), AccountType.SAVINGS, "EUR", BigDecimal.valueOf(0.02), LocalDate.of(2025, 3, 10))
        );
        accountRepository.saveAll(accounts);

        given()
                .when()
                .get("/accounts/balance/greater-than/{amount}", amount)
                .then()
                .statusCode(200)
                .body("owner", containsInAnyOrder("Anna Nowak", "Patryk Zmi", "Anna Nowak"))
                .body("owner.size()", equalTo(3))
                .body("balance", everyItem(greaterThan(2000F)));
    }

    @Test
    void shouldReturnNoAccountWithBalanceMoreThan() {
        BigDecimal amount = BigDecimal.valueOf(20000);
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(1000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5)),
                createAccount("Piotr Zieliński", BigDecimal.valueOf(500), AccountType.CHECKING, "EUR", BigDecimal.ZERO, LocalDate.of(2021, 3, 1)),
                createAccount("Jan Kowalski", BigDecimal.valueOf(2000), AccountType.SAVINGS, "PLN", BigDecimal.valueOf(0.02), LocalDate.of(2025, 1, 20)),
                createAccount("Patryk Zmi", BigDecimal.valueOf(10000), AccountType.CHECKING, "USD", BigDecimal.ZERO, LocalDate.of(2025, 2, 15)),
                createAccount("Anna Nowak", BigDecimal.valueOf(7000), AccountType.SAVINGS, "EUR", BigDecimal.valueOf(0.02), LocalDate.of(2025, 3, 10))
        );
        accountRepository.saveAll(accounts);

        given()
                .when()
                .get("/accounts/balance/greater-than/{amount}", amount)
                .then()
                .statusCode(404)
                .body("message", containsString("No accounts found with balance greater than"));
    }

    // TESTY DLA TRANSFERU, DEPOZYTU I WYPŁATY

    // /accounts/transfer → wykonanie przelewu
    @Test
    void shouldReturnCompletedTransfer() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(10000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5))
        );

        List<Account> savedAccounts = accountRepository.saveAll(accounts);
        Long senderId = savedAccounts.get(0).getId();
        Long receiverId = savedAccounts.get(1).getId();

        TransferRequest requestBody = new TransferRequest();
        requestBody.setSenderId(senderId);
        requestBody.setReceiverId(receiverId);
        requestBody.setAmount(BigDecimal.valueOf(200));

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(200)
                .body(equalTo("Transfer completed"));
        // Walidacja salda kont po transferze
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();

        assertThat(senderAfter.getBalance()).isEqualByComparingTo("9800");
        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("2700");
    }

    // za mała kwota transferu - musi być przynajmniej 0.01
    @Test
    void shouldReturnStatusCode400WhenAmountIsNotAtLeast001() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(10000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5))
        );

        List<Account> savedAccounts = accountRepository.saveAll(accounts);
        Long senderId = savedAccounts.get(0).getId();
        Long receiverId = savedAccounts.get(1).getId();

        TransferRequest requestBody = new TransferRequest();
        requestBody.setSenderId(senderId);
        requestBody.setReceiverId(receiverId);
        requestBody.setAmount(BigDecimal.valueOf(-200));

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(400)
                .body("message", containsString("Amount must be at least 0.01"))
                .body("status", equalTo(400));
        // Walidacja salda kont po transferze
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();

        assertThat(senderAfter.getBalance()).isEqualByComparingTo("10000");
        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("2500");
    }

    // SenderId jest równe zero
    @Test
    void shouldReturnStatusCode404WhenSenderIdIsZero() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(10000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5))
        );

        List<Account> savedAccounts = accountRepository.saveAll(accounts);
        Long senderId = savedAccounts.get(0).getId();
        Long receiverId = savedAccounts.get(1).getId();

        TransferRequest requestBody = new TransferRequest();
        requestBody.setSenderId(0L);
        requestBody.setReceiverId(receiverId);
        requestBody.setAmount(BigDecimal.valueOf(200));

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(404)
                .body("message", containsString("Account not found with id 0"))
                .body("status", equalTo(404));
        // Walidacja salda kont po transferze
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();

        assertThat(senderAfter.getBalance()).isEqualByComparingTo("10000");
        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("2500");
    }

    // ReceiverId jest równe 0
    @Test
    void shouldReturnStatusCode404WhenReceiverIdIsZero() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(10000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5))
        );

        List<Account> savedAccounts = accountRepository.saveAll(accounts);
        Long senderId = savedAccounts.get(0).getId();
        Long receiverId = savedAccounts.get(1).getId();

        TransferRequest requestBody = new TransferRequest();
        requestBody.setSenderId(senderId);
        requestBody.setReceiverId(0L);
        requestBody.setAmount(BigDecimal.valueOf(200));

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(404)
                .body("message", containsString("Account not found with id 0"))
                .body("status", equalTo(404));
        // Walidacja salda kont po transferze
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();

        assertThat(senderAfter.getBalance()).isEqualByComparingTo("10000");
        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("2500");
    }

    // To samo Id ma sender i receiver
    @Test
    void shouldReturnStatusCode400WhenSenderIdAndReceiverIdIsTheSame() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(10000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5))
        );

        List<Account> savedAccounts = accountRepository.saveAll(accounts);
        Long senderId = savedAccounts.get(0).getId();
        Long receiverId = savedAccounts.get(1).getId();

        TransferRequest requestBody = new TransferRequest();
        requestBody.setSenderId(senderId);
        requestBody.setReceiverId(senderId);
        requestBody.setAmount(BigDecimal.valueOf(200));

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(400)
                .body("message", containsString("Nie można wykonać przelewu na to samo konto"))
                .body("status", equalTo(400));
        // Walidacja salda kont po transferze
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();

        assertThat(senderAfter.getBalance()).isEqualByComparingTo("10000");
        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("2500");
    }

    // brak wystraczających środków na koncie
    @Test
    void shouldReturnStatusCode400WhenNotEnoughBalance() {
        List<Account> accounts = List.of(
                createAccount("Jan Kowalski", BigDecimal.valueOf(10000), AccountType.CHECKING, "PLN", BigDecimal.ZERO, LocalDate.of(2020, 1, 10)),
                createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5))
        );

        List<Account> savedAccounts = accountRepository.saveAll(accounts);
        Long senderId = savedAccounts.get(0).getId();
        Long receiverId = savedAccounts.get(1).getId();

        TransferRequest requestBody = new TransferRequest();
        requestBody.setSenderId(senderId);
        requestBody.setReceiverId(receiverId);
        requestBody.setAmount(BigDecimal.valueOf(20000));

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(400)
                .body("message", containsString("Brak wystarczających środków na koncie"))
                .body("status", equalTo(400));
        // Walidacja salda kont po transferze
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();

        assertThat(senderAfter.getBalance()).isEqualByComparingTo("10000");
        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("2500");
    }

    // Wpłata na konto /accounts/{id}/deposit
    @Test
    void shouldDepositMoney() {
        Account account = createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5));
        BigDecimal amount = BigDecimal.valueOf(200);
        BigDecimal expectedBalance = account.getBalance().add(amount);
        Account savedAccount = accountRepository.save(account);
        Long accountId = savedAccount.getId();

        given()
                .contentType(ContentType.JSON)
                .queryParam("amount", amount)
                .when()
                .post("/accounts/{id}/deposit", accountId)
                .then()
                .statusCode(200)
                .body("id", equalTo(accountId.intValue()))
                .body("balance", equalTo(expectedBalance.floatValue()));
        // walidacja w bazie po wpłacie
        Account accountAfter = accountRepository.findById(accountId).orElseThrow();
        assertThat(accountAfter.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    void shouldReturn400WhenDepositNegative() {
        Account account = createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5));

        Account savedAccount = accountRepository.save(account);
        Long accountId = savedAccount.getId();

        given()
                .contentType(ContentType.JSON)
                .queryParam("amount", -200)
                .when()
                .post("/accounts/{id}/deposit", accountId)
                .then()
                .statusCode(400)
                .body("message", containsString("Kwota musi być większa od 0"));

        // Walidacja salda konta po nieudanej operacji
        Account receiverAfter = accountRepository.findById(accountId).orElseThrow();

        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("2500");
    }

    // Wypłata z konta /accounts/{id}/withdarw
    @Test
    void shouldWithdrawMoney() {
        Account account = createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5));
        BigDecimal amount = BigDecimal.valueOf(200);
        BigDecimal expectedBalance = account.getBalance().subtract(amount);
        Account savedAccount = accountRepository.save(account);
        Long accountId = savedAccount.getId();

        given()
                .contentType(ContentType.JSON)
                .queryParam("amount", amount)
                .when()
                .post("/accounts/{id}/withdraw", accountId)
                .then()
                .statusCode(200)
                .body("id", equalTo(accountId.intValue()))
                .body("balance", equalTo(expectedBalance.floatValue()));
        // walidacja w bazie po wypłacie
        Account accountAfter = accountRepository.findById(accountId).orElseThrow();
        assertThat(accountAfter.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    void shouldReturn400WhenWithdrawTooMuch() {
        Account account = createAccount("Anna Nowak", BigDecimal.valueOf(2500), AccountType.SAVINGS, "USD", BigDecimal.valueOf(0.02), LocalDate.of(2025, 2, 5));
        Account savedAccount = accountRepository.save(account);
        Long accountId = savedAccount.getId();

        given()
                .contentType(ContentType.JSON)
                .queryParam("amount", 20000)
                .when()
                .post("/accounts/{id}/withdraw", accountId)
                .then()
                .statusCode(400)
                .body("message", containsString("Brak wystarczających środków na koncie"));

        // Walidacja salda konta po nieudanej operacji
        Account receiverAfter = accountRepository.findById(accountId).orElseThrow();

        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("2500");
    }



}
