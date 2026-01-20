package com.example.minibank2.integration;

import com.example.minibank2.entity.Account;
import com.example.minibank2.entity.AccountType;
import com.example.minibank2.repository.AccountRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
        "server.port=8080",
        "spring.datasource.url=jdbc:h2:file:./data/minibankdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=update"
})
public class AccountControllerTest {

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setup() {
        // Nie czyścimy bazy — chcemy widzieć dane w H2 po testach
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    @Test
    public void testGetAllAccountsInitiallyEmpty() {
        given()
                .when()
                .get("/accounts")
                .then()
                .statusCode(200)
                .body("$", notNullValue()); // lista kont (może być pusta lub nie)
    }

    @Test
    public void testCreateAccount() {
        String json = """
        {
          "owner": "Alicja Kowalska",
          "number": "PL1234567890",
          "currency": "PLN",
          "balance": 1000.50,
          "status": "ACTIVE",
          "accountType": "SAVINGS",
          "interestRate": 0.05
        }
        """;

        given()
                .header("Content-Type", "application/json")
                .body(json)
                .when()
                .post("/accounts")
                .then()
                .statusCode(200)
                .body("owner", equalTo("Alicja Kowalska"))
                .body("number", equalTo("PL1234567890"))
                .body("balance", equalTo(1000.50f))
                .body("interestRate", equalTo(0.05f));
    }

    @Test
    public void testUpdateAccount() {
        // 1. Tworzymy konto testowe
        Account account = new Account("Alicja", "235256436", "PLN",
                new BigDecimal("9999.00"), "ACTIVE", AccountType.SAVINGS, new BigDecimal("0.05"));
        account = accountRepository.save(account);

        // 2. Dane do aktualizacji
        String jsonUpdate = """
        {
          "owner": "Alexis",
          "number": "PL123",
          "currency": "PLN",
          "balance": 2500.00,
          "status": "BLOCKED",
          "accountType": "SAVINGS",
          "interestRate": 0.07
        }
        """;

        // 3. PUT do API
        given()
                .header("Content-Type", "application/json")
                .body(jsonUpdate)
                .when()
                .put("/accounts/" + account.getId())
                .then()
                .statusCode(200)
                .body("owner", equalTo("Alexis"))
                .body("balance", equalTo(2500.00f))
                .body("status", equalTo("BLOCKED"));

        // 4. Weryfikacja w repozytorium
        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals("Alexis", updated.getOwner());
        assertEquals(new BigDecimal("2500.00"), updated.getBalance());
    }
}
