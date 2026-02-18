package com.example.minibank2.integration.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")

public class TransactionE2ETest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    // ---------------------- SCENARIUSZ 1 ----------------------
    // Happy path – transfer działa poprawnie
    @Test
    void shouldTransferMoneyBetweenAccounts_E2E() {

        // Utworzenie kont
        Long senderId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                    {
                                      "owner": "Anna Nowak",
                                      "currency": "USD",
                                      "accountType": "SAVINGS"
                                    }
                                """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        Long receiverId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                    {
                                      "owner": "Jan Kowalski",
                                      "currency": "USD",
                                      "accountType": "SAVINGS"
                                    }
                                """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        // Deposit na konto nadawcy
        BigDecimal depositAmount = BigDecimal.valueOf(1000);

        given()
                .queryParam("amount", depositAmount)
                .when()
                .post("/accounts/{id}/deposit", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        // Transfer
        BigDecimal transferAmount = BigDecimal.valueOf(400);

        given()
                .contentType(ContentType.JSON)
                .body("""
                            {
                              "senderId": %d,
                              "receiverId": %d,
                              "amount": %s
                            }
                        """.formatted(senderId, receiverId, transferAmount))
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(200)
                .body(equalTo("Transfer completed"));

        // Asercje końcowe
        //Saldo nadawcy
        given()
                .when()
                .get("/accounts/{id}", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(
                        depositAmount.subtract(transferAmount).floatValue()
                ));

        // Saldo odbiorcy
        given()
                .when()
                .get("/accounts/{id}", receiverId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(transferAmount.floatValue()));

        // Historia transakcji
        given()
                .when()
                .get("/transactions/{accountId}", senderId)
                .then()
                .statusCode(200)
                .body("type", hasItem("TRANSFER_OUT"))
                .body("amount", hasItem(transferAmount.floatValue()));

        given()
                .when()
                .get("/transactions/{accountId}", receiverId)
                .then()
                .statusCode(200)
                .body("type", hasItem("TRANSFER_IN"))
                .body("amount", hasItem(transferAmount.floatValue()));

    }

    // ---------------------- SCENARIUSZ 2 ----------------------
    // Transfer fail – brak wystarczających środków
    @Test
    void shouldFailTransfer_WhenInsufficientFunds() {
        // Utworzenie kont
        Long senderId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Anna Nowak",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        Long receiverId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Patryk Zmit",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        // Depozyt nadawcy
        BigDecimal depositAmount = BigDecimal.valueOf(100);

        given()
                .queryParam("amount", depositAmount)
                .when()
                .post("/accounts/{id}/deposit", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        // Transfer
        BigDecimal transferAmount = BigDecimal.valueOf(500);

        given()
                .contentType(ContentType.JSON)
                .body("""
                            {
                              "senderId": %d,
                              "receiverId": %d,
                              "amount": %s
                            }
                        """.formatted(senderId, receiverId, transferAmount))
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(400)
                .body("message", containsString("Brak wystarczających środków na koncie"));

        // Sprawdzenie sald obu kont - nie powinny się zmienić

        // Saldo nadawcy

        given()
                .when()
                .get("/accounts/{id}", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        // Saldo odbiorcy
        given()
                .when()
                .get("/accounts/{id}", receiverId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(0F));
    }

    // ---------------------- SCENARIUSZ 3 ----------------------
    // Transfer fail – wysyłka na to samo konto
    @Test
    void shouldFailTransfer_WhenSenderEqualsReceiver() {
        // Utworzenie kont
        Long senderId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Anna Nowak",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        Long receiverId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Patryk Zmit",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        // Dopozyt na konto nadawcy
        BigDecimal depositAmount = BigDecimal.valueOf(1000);

        given()
                .queryParam("amount", depositAmount)
                .when()
                .post("/accounts/{id}/deposit", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        // Transfer na to samo konto
        BigDecimal transferAmount = BigDecimal.valueOf(500);

        given()
                .contentType(ContentType.JSON)
                .body("""
                            {
                              "senderId": %d,
                              "receiverId": %d,
                              "amount": %s
                            }
                        """.formatted(senderId, senderId, transferAmount))
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(400)
                .body("message", containsString("Nie można wykonać przelewu na to samo konto"));

        // Walidacja sald kont - nie powinny sie zmienić

        given()
                .when()
                .get("/accounts/{id}", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        given()
                .when()
                .get("/accounts/{id}", receiverId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(0F));
    }

    @Test
    void shouldFailTransfer_WhenAmountIsZero() {
        // Utworzenie kont
        Long senderId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Anna Nowak",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        Long receiverId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Patryk Zmit",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        // Dopozyt na konto nadawcy
        BigDecimal depositAmount = BigDecimal.valueOf(500);

        given()
                .queryParam("amount", depositAmount)
                .when()
                .post("/accounts/{id}/deposit", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        // Transfer
        BigDecimal transferAmount = BigDecimal.valueOf(0);

        given()
                .contentType(ContentType.JSON)
                .body("""
                            {
                              "senderId": %d,
                              "receiverId": %d,
                              "amount": %s
                            }
                        """.formatted(senderId, receiverId, transferAmount))
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(400)
                .body("message", containsString("Amount must be at least 0.01"));

        // Walidacja sald kont - nie powinny sie zmienić

        given()
                .when()
                .get("/accounts/{id}", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        given()
                .when()
                .get("/accounts/{id}", receiverId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(0F));

        // Historia transakcji – tylko DEPOSIT
        given()
                .when()
                .get("/transactions/{accountId}", senderId)
                .then()
                .statusCode(200)
                .body("type", hasItem("DEPOSIT"))
                .body("type", not(hasItem("TRANSFER_OUT")))
                .body("type", not(hasItem("TRANSFER_IN")));
    }

    // ---------------------- SCENARIUSZ 4 ----------------------
    // Sprawdzenie historii transakcji
    @Test
    void shouldRecordAllTransactionTypesCorrectly() {
        // Utworzenie kont
        Long senderId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Anna Nowak",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        Long receiverId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Patryk Zmit",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        // Dopozyt na konto nadawcy
        BigDecimal depositAmount = BigDecimal.valueOf(500);

        given()
                .queryParam("amount", depositAmount)
                .when()
                .post("/accounts/{id}/deposit", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        BigDecimal depositAmount2 = BigDecimal.valueOf(200);
        BigDecimal totalDepositAmount = depositAmount.add(depositAmount2);

        given()
                .queryParam("amount", depositAmount2)
                .when()
                .post("/accounts/{id}/deposit", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(totalDepositAmount.floatValue()));

        // Wypłata z konta
        BigDecimal withdrawAmount = BigDecimal.valueOf(300);

        given()
                .queryParam("amount", withdrawAmount)
                .when()
                .post("/accounts/{id}/withdraw", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(totalDepositAmount.subtract(withdrawAmount).floatValue()));

        // Transfer
        BigDecimal transferAmount = BigDecimal.valueOf(150);
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "senderId": %d,
                          "receiverId": %d,
                          "amount": %s
                        }
                        """.formatted(senderId, receiverId, transferAmount))
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(200)
                .body(equalTo("Transfer completed"));

        // Sprawdzenie historii nadawcy
        given()
                .when().get("/transactions/{accountId}", senderId)
                .then()
                .statusCode(200)
                .body("type", hasItem("DEPOSIT"))
                .body("type", hasItem("WITHDRAW"))
                .body("type", hasItem("TRANSFER_OUT"))
                .body("amount", hasItem(depositAmount.floatValue()))
                .body("amount", hasItem(depositAmount2.floatValue()))
                .body("amount", hasItem(transferAmount.floatValue()))
                .body("amount", hasItem(withdrawAmount.floatValue()));

        // Sprawdzenie historii odbiorcy
        given()
                .when().get("/transactions/{accountId}", receiverId)
                .then()
                .statusCode(200)
                .body("type", hasItem("TRANSFER_IN"))
                .body("amount", hasItem(transferAmount.floatValue()));
    }

    // Test filtrów transakcji
    @Test
    void shouldReturnOnlyDepositsAndTransactionsAboveGivenAmount
    () {
        // Utworzenie konta
        Long senderId =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                """ 
                                        {
                                        "owner": "Anna Nowak",
                                        "currency": "PLN",
                                        "accountType": "SAVINGS"
                                        }
                                        """)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        BigDecimal depositAmount = BigDecimal.valueOf(1000);

        given()
                .queryParam("amount", depositAmount)
                .when()
                .post("/accounts/{id}/deposit", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(depositAmount.floatValue()));

        BigDecimal depositAmount2 = BigDecimal.valueOf(500);
        BigDecimal totalDepositAmount = depositAmount.add(depositAmount2);

        given()
                .queryParam("amount", depositAmount2)
                .when()
                .post("/accounts/{id}/deposit", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(totalDepositAmount.floatValue()));

        // Wypłata z konta
        BigDecimal withdrawAmount1 = BigDecimal.valueOf(300);
        BigDecimal actuallyBalance = BigDecimal.valueOf(totalDepositAmount.subtract(withdrawAmount1).floatValue());

        given()
                .queryParam("amount", withdrawAmount1)
                .when()
                .post("/accounts/{id}/withdraw", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(actuallyBalance.floatValue()));

        BigDecimal withdrawAmount2 = BigDecimal.valueOf(100);
        BigDecimal finallyBalance = BigDecimal.valueOf(actuallyBalance.subtract(withdrawAmount2).floatValue());

        given()
                .queryParam("amount", withdrawAmount2)
                .when()
                .post("/accounts/{id}/withdraw", senderId)
                .then()
                .statusCode(200)
                .body("balance", equalTo(finallyBalance.floatValue()));

        // Asercje końcowe dle endpointów Deposit i Above
        String transactionType = "DEPOSIT";
        given()
                .queryParam("type", transactionType)
                .when()
                .get("/transactions/{accountId}/type", senderId)
                .then()
                .statusCode(200)
                .body("type", everyItem(equalTo("DEPOSIT")));

        BigDecimal amount = BigDecimal.valueOf(200);
        given()
                .queryParam("amount", amount)
                .when()
                .get("/transactions/{accountId}/above", senderId)
                .then()
                .statusCode(200)
                .body("amount", everyItem(greaterThan(amount.floatValue())));
    }
}
