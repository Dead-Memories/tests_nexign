package com.Nexign;

import com.Nexign.e2e.Utils.DbHelper;
import com.Nexign.e2e.Utils.TarifficationRequestGenerator;
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
    private static final int MONTHLY_TARIFF_ID = 12;
    private static final int CLASSIC_TARIFF_ID = 11;

    @Override
    protected int getPort() {
        return 8082;
    }

    @DisplayName("POST /tarifficateCall - запрос тарификации с корректным телом запроса")
    @Test
    public void tarifficateCall_validRequest() {
        TarifficationRequestGenerator.RequestResult rr = TarifficationRequestGenerator.generate();
        given()
                .contentType(ContentType.JSON)
                .body(rr.body)
                .when()
                .post("/tarifficateCall")
                .then()
                .statusCode(200)
                .body("tariffBalanceChange", equalTo(rr.expectedTariffBalanceChange))
                .body("balanceChange", equalTo(rr.expectedBalanceChange));
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
    public void monthTariffication_validId() throws Exception {

        int monthlyMinuteCapacity = DbHelper.selectInt(
                "hrs",
                "SELECT monthly_minute_capacity FROM tariff_parameter WHERE tariff_type_id =2 ",
                "monthly_minute_capacity"
        );
        float monthlyFee = DbHelper.selectFloat(
                "hrs",
                "SELECT monthly_fee FROM tariff_parameter WHERE tariff_type_id =2",
                "monthly_fee"
        );

        Response response = given()
                .when()
                .get("/monthTariffication/" + MONTHLY_TARIFF_ID);
        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("tariffBalanceChange", equalTo(monthlyMinuteCapacity))
                .body("balanceChange", equalTo(-monthlyFee));
    }

    @DisplayName("GET /monthTariffication/11 - запрос помесячной тарификации для Классического тарифа")
    @Test
    public void monthTariffication_invalidId() {
        given()
                .when()
                .get("/monthTariffication/" + CLASSIC_TARIFF_ID)
                .then()
                .statusCode(409)
                .body(equalTo("Помесячная тарификация не предрставляется для этого тарифа"));
    }
}
