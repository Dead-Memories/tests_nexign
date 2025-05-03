package com.Nexign;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

/**
 * Общее поведение для всех интеграционных тестов REST Assured:
 * – ставит базовый URL
 * – берёт порт у конкретного теста
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {
    @BeforeAll
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port    = getPort();
    }

    /** Каждый сабкласс возвращает свой порт */
    protected abstract int getPort();
}