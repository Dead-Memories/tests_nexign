package com.Nexign;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Автотесты для HRS-сервиса по эндпоинтам /tarifficateCall и /monthTariffication
 */
public class HrsApiTests extends BaseTest {
    @Override
    protected int getPort() {
        return 8082;
    }

    @DisplayName("POST /tarifficateCall - запрос тарификации с корректным телом запроса")
    @Test
    public void tarifficateCall_validRequest() {
        Map<String, Object> body = Map.of(
                "minutes", 10,
                "callType", 1,
                "isRomashkaCall", 1,
                "tariffId", 12,
                "tariffBalance", 6,
                "balance", 0.0
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/tarifficateCall");

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("tariffBalanceChange", equalTo(-6))
                .body("balanceChange", equalTo(-6.0F));
    }

    @DisplayName("POST /tarifficateCall - запрос тарификации для несуществующего тарифа")
    @Test
    public void tarifficateCall_invalidTariffId() {
        Map<String, Object> body = Map.of(
                "minutes", 10,
                "callType", 1,
                "isRomashkaCall", 1,
                "tariffId", 9999,
                "tariffBalance", 6,
                "balance", 0.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/tarifficateCall")
                .then()
                .statusCode(404)
                .body(equalTo("Тариф с таким id не был найден"));
    }

    @DisplayName("GET /monthTariffication/12 - запрос помесячной тарификации для Помесячного тарифа ")
    @Test
    public void monthTariffication_validId() {
        Response response = given()
                .when()
                .get("/monthTariffication/12");

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("tariffBalanceChange", equalTo(50))
                .body("balanceChange", equalTo(-100.0F));
    }

//    TODO: добавить запрос параметров tariffBalanceChange и balanceChange из БД на случай, если параметры тарифа изменятся

    @DisplayName("GET /monthTariffication/11 - запрос помесячной тарификации для Классического тарифа")
    @Test
    public void monthTariffication_invalidId() {
        given()
                .when()
                .get("/monthTariffication/11")
                .then()
                .statusCode(409)
                .body(equalTo("Помесячная тарификация не предрставляется для этого тарифа"));
    }
}
