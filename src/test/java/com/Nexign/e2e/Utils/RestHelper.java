package com.Nexign.e2e.Utils;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;

public class RestHelper {
    public static void processCdr(String body) {
        Response response = given()
                .contentType(JSON)
                .body(body)
                .when()
                .post("/processCdrList");

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("", hasSize(1));

    }
}
