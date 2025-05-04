package com.Nexign;

import com.Nexign.BaseTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Позитивные CRUD-тесты для Subscriber API (CRM)
 * Тесты выполняются в последовательности и пропускаются при падении предыдущего шага
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("sequence")
public class SubscriberCrudTest extends BaseTest {
    private static final String MSISDN = "79991234567";
    private boolean createPassed;

    @Override
    protected int getPort() {
        return 8083;
    }

    @Order(1)
    @DisplayName("Создание абонента с корректными данными")
    @Test
    void createSubscriber() {
        Map<String, Object> request = Map.of(
                "fullName", "Иванов Иван Иванович",
                "passport", "11040000007",
                "dateOfBirth", "2002-07-30",
                "msisdn", MSISDN,
                "tariff", "Классический",
                "balance", 100
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/manager/subscriber/add");

        response.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("fullName", equalTo(request.get("fullName")))
                .body("passport", equalTo(request.get("passport")))
                .body("dateOfBirth", equalTo(request.get("dateOfBirth")))
                .body("msisdn", equalTo(request.get("msisdn")))
                .body("tariff", equalTo(request.get("tariff")))
                .body("balance", equalTo(request.get("balance")));

        createPassed = true;
    }

    @Order(2)
    @DisplayName("Получение информации о пользователе")
    @Test
    void getSubscriberFullInfo() {
        assumeTrue(createPassed, "Create step failed, skipping FullInfo test");

        Response response = given()
                .when()
                .get("/manager/subscriber/{msisdn}/fullinfo", MSISDN);

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("fullName", equalTo("Сергеев Иван Сергеевич"))
                .body("passport", equalTo("11040000037"))
                .body("dateOfBirth", equalTo("2002-07-30"))
                .body("msisdn", equalTo(MSISDN))
                .body("tariff", equalTo("Классический"))
                .body("balance", equalTo(100))
                .body("lastChargeDate", equalTo("2025-04-20"))
                .body("minutes", equalTo(130));
    }

    @Order(3)
    @DisplayName("Обновление личной информации пользователя")
    @Test
    void updateSubscriber() {
        assumeTrue(createPassed, "Create step failed, skipping Update test");

        Map<String, Object> request = Map.of(
                "fullName", "Иванов Иван Иванович",
                "passport", "11040000007",
                "dateOfBirth", "2002-07-30",
                "msisdn", MSISDN,
                "tariff", "Классический",
                "balance", 100
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .patch("/manager/subscriber/{msisdn}/update", MSISDN);

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("fullName", equalTo("Сергеев Иван Сергеевич"))
                .body("passport", equalTo("11040000037"))
                .body("dateOfBirth", equalTo("2002-07-30"))
                .body("msisdn", equalTo(MSISDN))
                .body("tariff", equalTo("Классический"))
                .body("balance", equalTo(100));
    }

    @Order(4)
    @DisplayName("Удаление пользователя")
    @Test
    void deleteSubscriber() {
        assumeTrue(createPassed, "Create step failed, skipping Delete test");

        Response response = given()
                .when()
                .delete("/manager/subscriber/{msisdn}/delete", MSISDN);

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(MSISDN))
                .body("status", equalTo("deleted"));
    }
}
