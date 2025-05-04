package com.Nexign;

import com.Nexign.BaseTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Тесты для сервиса CRM: тарифные операции
 * 79991234567 - тестовый номер. Он у нас всегда лежит в базе и на нем можно проверять операции
 */
public class TariffApiTest extends BaseTest {
    private static final String MSISDN = "79991234567";

    @Override
    protected int getPort() {
        return 8083;
    }

    @DisplayName("GET /manager/subscriber/{msisdn}/gettariff - получение тарифа у тестового номера")
    @Test
    public void getTariff() {
        given()
                .when()
                .get("/manager/subscriber/{msisdn}/gettariff", MSISDN)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(MSISDN))
                .body("currentTariff", equalTo("Классический"))
                .body("availableTariffs", hasItems("Классический", "Помесячный"));
    }

    @DisplayName("PUT /manager/subscriber/{msisdn}/changetariff - смена тарифа у тестового номера")
    @Test
    public void changeTariff_shouldReturn201_andTariffChange() {
        Map<String, Object> request = Map.of(
                "msisdn", MSISDN,
                "currentTariff", "Классический",
                "availableTariffs", List.of("Классический", "Помесячный"),
                "newTariff", "Помесячный"
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/manager/subscriber/{msisdn}/changetariff", MSISDN);

        response.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(MSISDN))
                .body("currentTariff", equalTo("Помесячный"))
                .body("availableTariffs", hasSize(2))
                .body("availableTariffs", hasItems("Классический", "Помесячный"));
    }
}

