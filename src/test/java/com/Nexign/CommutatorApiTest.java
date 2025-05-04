package com.Nexign;

import com.Nexign.BaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.emptyString;

/**
 * Автотесты для сервиса Коммутатор по эндпоинтам /generate и /truncate
 */
public class CommutatorApiTest extends BaseTest {
    @Override
    protected int getPort() {
        return 8080;
    }

    @DisplayName("POST /generate - первый вызов успешно генерирует случайное число звонков")
    @Test
    public void generateFirstTime() {
        Response response = given()
                .when()
                .post("/generate");

        response.then()
                .statusCode(200)
                .body(matchesPattern("\\d+ calls generated successfully"));
    }

    @DisplayName("DELETE /truncate - удаляет все записи")
    @Test
    public void truncate() {
        Response response = given()
                .when()
                .delete("/truncate");

        response.then()
                .statusCode(200)
                .body(emptyString());
    }

    @DisplayName("POST /generate - второй вызов возвращает 208 и говорит, что данные уже сгенерированы")
    @Test
    public void generateSecondTime() {
        // Первое выполнение для инициализации данных
        given()
                .when()
                .post("/generate");

        // Второе выполнение — ожидаем 208
        Response second = given()
                .when()
                .post("/generate");

        second.then()
                .statusCode(208)
                .body(equalTo("Data Already Generated"));
    }
}
