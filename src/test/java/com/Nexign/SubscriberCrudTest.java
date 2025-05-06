package com.Nexign;

import com.Nexign.BaseTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Позитивные и негативные CRUD-тесты для Subscriber API (CRM) с рандомными входными данными.
 */

public class SubscriberCrudTest extends BaseTest {
    private Map<String, Object> subscriberData;
    private String msisdn;
    private final Random random = new Random();

    @Override
    protected int getPort() {
        return 8083;
    }

    @BeforeAll
    void initSubscriberData() {
        // Генерация случайных данных для абонента
        String fullName = "User" + random.nextInt(10000) + " Test";
        String passport = String.valueOf(1000000000L + random.nextInt(900000000));
        String dateOfBirth = String.format("1980-01-%02d", random.nextInt(28) + 1);
        msisdn = "7" + String.format("%010d", Math.abs(random.nextLong()) % 1_000_000_0000L);
        String tariff = random.nextBoolean() ? "Классический" : "Помесячный";
        float balance = Math.round(random.nextFloat() * 1000f * 100) / 100f;

        subscriberData = Map.of(
                "fullName", fullName,
                "passport", passport,
                "dateOfBirth", dateOfBirth,
                "msisdn", msisdn,
                "tariff", tariff,
                "balance", balance
        );
    }

    @DisplayName("Создание абонента - позитивный кейс")
    @Test
    void createSubscriber_positive() {
        Response resp = given()
                .contentType(ContentType.JSON)
                .body(subscriberData)
                .when()
                .post("/manager/subscriber/add");

        resp.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("msisdn", equalTo(msisdn))
                .body("fullName", equalTo(subscriberData.get("fullName")))
                .body("passport", equalTo(subscriberData.get("passport")))
                .body("dateOfBirth", equalTo(subscriberData.get("dateOfBirth")))
                .body("tariff", equalTo(subscriberData.get("tariff")))
                .body("balance", equalTo(subscriberData.get("balance")));
    }

    @DisplayName("Создание абонента - негативный кейс")
    @Test
    void createSubscriber_negative() {
        // Удаляем обязательное поле passport
        Map<String, Object> badData = Map.of(
                "fullName", subscriberData.get("fullName"),
                "dateOfBirth", subscriberData.get("dateOfBirth"),
                "msisdn", subscriberData.get("msisdn"),
                "tariff", subscriberData.get("tariff"),
                "balance", subscriberData.get("balance")
        );

        given()
                .contentType(ContentType.JSON)
                .body(badData)
                .when()
                .post("/manager/subscriber/add")
                .then()
                .statusCode(400)
                .body("explaination", not(emptyString()));
    }

    @DisplayName("Изменение данных - позитивный кейс")
    @Test
    void updateSubscriber_positive() {
        // Изменяем fullName
        String newName = subscriberData.get("fullName") + " Updated";
        Map<String, Object> updateData = Map.of(
                "fullName", newName,
                "passport", subscriberData.get("passport"),
                "dateOfBirth", subscriberData.get("dateOfBirth"),
                "msisdn", subscriberData.get("msisdn"),
                "tariff", subscriberData.get("tariff"),
                "balance", subscriberData.get("balance")
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateData)
                .when()
                .patch("/manager/subscriber/{msisdn}/update", msisdn)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("fullName", equalTo(newName));
    }

    @DisplayName("Изменение данных - негативный кейс ")
    @Test
    void updateSubscriber_negative() {
        String fake = msisdn + "99";
        given()
                .contentType(ContentType.JSON)
                .body(subscriberData)
                .when()
                .patch("/manager/subscriber/{msisdn}/update", fake)
                .then()
                .statusCode(404)
                .body("explaination", not(emptyString()));
    }

    @DisplayName("Удаление абонента - позитивный кейс")
    @Test
    void deleteSubscriber_positive() {
        given()
                .when()
                .delete("/manager/subscriber/{msisdn}/delete", msisdn)
                .then()
                .statusCode(200)
                .body("msisdn", equalTo(msisdn))
                .body("status", equalTo("deleted"));
    }

    @DisplayName("Удаление абонента - негативный кейс")
    @Test
    void deleteSubscriber_negative() {
        String fake = msisdn + "99";
        given()
                .when()
                .delete("/manager/subscriber/{msisdn}/delete", fake)
                .then()
                .statusCode(404)
                .body("explaination", not(emptyString()));
    }
}
