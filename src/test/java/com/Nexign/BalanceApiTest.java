package com.Nexign;

import com.Nexign.BaseTest;
import com.Nexign.e2e.Utils.DbHelper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Тесты для CRM-сервиса: операции с балансом абонента
 */
public class BalanceApiTest extends BaseTest {
    private static String msisdn;
    private static float balance;
    private static List<String> allMsisdns;
    private static String badMsisdn;
    private static final Random RANDOM = new Random();

    @Override
    protected int getPort() {
        return 8083;
    }

    @BeforeAll
    static void setupSubscriber() throws Exception {

        // Выбираем произвольного абонента из BRT
        List<String> list = DbHelper.selectColumn(
                "brt", "SELECT msisdn FROM subscriber LIMIT 1", "msisdn"
        );
        assertFalse(list.isEmpty(), "BRT база пуста: нет ни одного подписчика");
        msisdn = list.get(0);

        // Фиксируем его баланс для проверки
        balance = DbHelper.selectFloat(
                "brt",
                "SELECT balance FROM subscriber WHERE msisdn='" + msisdn + "'",
                "balance"
        );

        // Генерируем несуществующий msisdn для негативных тестов
        allMsisdns = DbHelper.selectColumn(
                "brt", "SELECT msisdn FROM subscriber", "msisdn"
        );
        do {
            // Генерируем строку 11 цифр, начинающуюся с 7
            badMsisdn = "7" + String.format("%010d", Math.abs(RANDOM.nextLong()) % 10000000000L);
        } while (allMsisdns.contains(badMsisdn));
    }

    @DisplayName("GET /subscriber/{msisdn}/getbalance - с существующим msisdn")
    @Test
    public void getBalance_valid() {
        // Выполняем запрос на баланс
        Response response = given()
                .when()
                .get("/subscriber/{msisdn}/getbalance", msisdn);

        // Проверяем статус и совпадение баланса с БД
        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(msisdn))
                .body("balance", equalTo(balance))
                .body("tariff", not(emptyString()))
                .body("fullName", not(emptyString()))
                .body("lastChargeDate", not(emptyString()))
                .body("minutes", not(emptyString()));
    }

    @DisplayName("GET /subscriber/{msisdn}/getbalance - с несуществующим msisnd")
    @Test
    public void getBalance_not_valid() {
        given()
                .when()
                .get("/subscriber/{msisdn}/getbalance", badMsisdn)
                .then()
                .statusCode(404)
                .body("explaination", not(emptyString()));
    }

    @DisplayName("PUT /subscriber/{msisdn}/changebalance - с тестовым msisdn")
    @Test
    public void changeBalance_valid() throws Exception {
        // Генерируем случайную сумму пополнения (.nextInt(1000)+1 => 1..1000)
        int amount = RANDOM.nextInt(1000) + 1;

        // Подготовка тела запроса
        Map<String, Object> request = Map.of(
                "amount", amount,
                "paymentMethod", "bank_card"
        );

        // Выполняем запрос
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/subscriber/{msisdn}/changebalance", msisdn);

        // Проверка ответа
        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(msisdn))
                .body("amount", equalTo(amount))
                .body("paymentMethod", equalTo("bank_card"))
                .body("tariff", not(emptyString()))
                .body("fullName", not(emptyString()))
                .body("lastChargeDate", not(emptyString()))
                .body("minutes", not(emptyString()));

        // Проверка фактического баланса в БД: баланс должен увеличиться на amount
        float balanceAfter = DbHelper.selectFloat(
                "brt",
                "SELECT balance FROM subscriber WHERE msisdn='" + msisdn + "'",
                "balance"
        );
        assertEquals(balance + amount, balanceAfter, 0.01,
                "Баланс в БД должен увеличиться на сумму пополнения");
    }

    @DisplayName("PUT /subscriber/{msisdn}/changebalance - с несуществующим msisnd")
    @Test
    public void changeBalance_nonexistent() {
        Map<String, Object> request = Map.of(
                "amount", 500,
                "paymentMethod", "bank_card"
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/subscriber/{msisdn}/changebalance", badMsisdn)
                .then()
                .statusCode(404)
                .body("explaination", not(emptyString()));
    }
}
