package com.Nexign;

import com.Nexign.BaseTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Тесты для CRM-сервиса: операции с балансом абонента
 */
public class BalanceApiTest extends BaseTest {
    private static final String MSISDN = "79991234567";
    private static final String BAD_MSISDN = "7911111111";

    @Override
    protected int getPort() {
        return 8083;
    }

    @DisplayName("GET /subscriber/{msisdn}/getbalance - с тестовым msisdn")
    @Test
    public void getBalance_valid() {
        Response response = given()
                .when()
                .get("/subscriber/{msisdn}/getbalance", MSISDN);

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(MSISDN))
                .body("balance", not(emptyString()))
                .body("tariff", not(emptyString()))
                .body("fullName", equalTo("Иванов Иван Иванович"))
                .body("lastChargeDate", not(emptyString()))
                .body("minutes", not(emptyString()));
    }

    @DisplayName("GET /subscriber/{msisdn}/getbalance - с несуществующим msisnd")
    @Test
    public void getBalance_not_valid() {
        given()
                .when()
                .get("/subscriber/{msisdn}/getbalance", BAD_MSISDN)
                .then()
                .statusCode(404)
                .body("explaination", not(emptyString()));
    }

    @DisplayName("PUT /subscriber/{msisdn}/changebalance - с тестовым msisdn")
    @Test
    public void changeBalance_valid() {
        Map<String, Object> request = Map.of(
                "amount", 500,
                "paymentMethod", "bank_card"
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/subscriber/{msisdn}/changebalance", MSISDN);

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(MSISDN))
                .body("balance", not(emptyString()))
                .body("amount", equalTo(500))
                .body("paymentMethod", equalTo("bank_card"))
                .body("tariff", not(emptyString()))
                .body("fullName", equalTo("Иванов Иван Иванович"))
                .body("lastChargeDate", not(emptyString()))
                .body("minutes", not(emptyString()));
    }

    @DisplayName("PUT /subscriber/{msisdn}/changebalance - с несуществующим msisnd")
    @Test
    public void changeBalance_nonexistent_shouldReturn404_andError() {
        Map<String, Object> request = Map.of(
                "amount", 500,
                "paymentMethod", "bank_card"
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/subscriber/{msisdn}/changebalance", BAD_MSISDN)
                .then()
                .statusCode(404)
                .body("explaination", not(emptyString()));
    }
}
