package com.Nexign;

import com.Nexign.e2e.Utils.DbHelper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Тесты для сервиса CRM: тарифные операции
 */
public class TariffApiTest extends BaseTest {
    private static String msisdn;
    private static String currentTariff;
    private static String otherTariff;

    @Override
    protected int getPort() {
        return 8083;
    }

    @BeforeAll
    static void setupSubscriber() throws Exception {
        // 1) Выбираем произвольного абонента из BRT
        List<String> list = DbHelper.selectColumn(
                "brt", "SELECT msisdn FROM subscriber LIMIT 1", "msisdn"
        );
        assertFalse(list.isEmpty(), "BRT база пуста: нет ни одного подписчика");
        msisdn = list.get(0);

        // 2) Читаем его текущий тариф
        List<String> tariffs = DbHelper.selectColumn(
                "brt",
                "SELECT tariff FROM subscriber WHERE msisdn='" + msisdn + "'",
                "tariff"
        );
        assertFalse(tariffs.isEmpty(), "Не удалось получить тариф для msisdn=" + msisdn);
        currentTariff = tariffs.get(0);

        // 3) Определяем альтернативный тариф (предполагаем, что их два)
        otherTariff = currentTariff.equals("Классический") ? "Помесячный" : "Классический";
    }

    @DisplayName("GET /manager/subscriber/{msisdn}/gettariff - получение тарифа у тестового номера")
    @Test
    void getTariff() throws Exception {

        // 3) Вызываем API и сравниваем с данными из БД
        given()
                .pathParam("msisdn", msisdn)
                .when()
                .get("/manager/subscriber/{msisdn}/gettariff")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(msisdn))
                .body("currentTariff", equalTo(currentTariff))
                .body("availableTariffs", not(empty()));
    }

    @DisplayName("PUT /manager/subscriber/{msisdn}/changetariff - смена тарифа у тестового номера")
    @Test
    public void changeTariff() throws Exception {

        // 4) Подготовка и выполнение запроса смены тарифа
        Map<String,Object> changeReq = Map.of(
                "msisdn", msisdn,
                "currentTariff", currentTariff,
                "availableTariffs", List.of(currentTariff, otherTariff),
                "newTariff", otherTariff
        );

        try {
            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(changeReq)
                    .when()
                    .put("/manager/subscriber/{msisdn}/changetariff", msisdn);

            response.then()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("msisdn", equalTo(msisdn))
                    .body("currentTariff", equalTo(otherTariff))
                    .body("availableTariffs", hasSize(2))
                    .body("availableTariffs", hasItems(currentTariff, otherTariff));
        } finally {
            // 5) Восстанавливаем исходный тариф, чтобы не портить данные
            Map<String,Object> revertReq = Map.of(
                    "msisdn", msisdn,
                    "currentTariff", otherTariff,
                    "availableTariffs", List.of(otherTariff, currentTariff),
                    "newTariff", currentTariff
            );
            given()
                    .contentType(ContentType.JSON)
                    .body(revertReq)
                    .when()
                    .put("/manager/subscriber/{msisdn}/changetariff", msisdn)
                    .then()
                    .statusCode(200);
        }
    }

    @DisplayName("PUT /manager/subscriber/{msisdn}/changetariff - смена на тот же тариф, без изменений")
    @Test
    public void changeToSameTariff() {
        // Подготовка запроса, где newTariff == currentTariff
        Map<String,Object> sameReq = Map.of(
                "msisdn", msisdn,
                "currentTariff", currentTariff,
                "availableTariffs", List.of(currentTariff, otherTariff),
                "newTariff", currentTariff
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(sameReq)
                .when()
                .put("/manager/subscriber/{msisdn}/changetariff", msisdn);

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(msisdn))
                .body("currentTariff", equalTo(currentTariff))
                .body("availableTariffs", hasItems(currentTariff, otherTariff));
    }
}

