package com.Nexign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.Nexign.BaseTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Интеграционный тест для BRT: проверка endpoint /processCdrList
 * При отправке валидного CDR-файла в теле запроса проверяем:
 * - статус 200
 * - количество записей в ответе
 * <p>
 * При отправке невалидного CDR-файла в теле запроса проверяем:
 * - статус 200
 * - тело ответа пустое
 * <p>
 * В качестве файлов с тестовыми данными использую CDR, сгенерированные на предыдущих неделях
 */
public class ProcessCdrListTest extends BaseTest {

    @Override
    protected int getPort() {
        return 8081;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CDR_RESOURCE_DIR =
            "/Users/olya/IdeaProjects/tests_nexign/src/test/java/com/Nexign/TestResources";

    @Test
    @DisplayName("Тест с валидным CDR")
    void processValidCdrFile() throws Exception {
        // Загружаем валидный CDR-файл
        File file = Paths.get(CDR_RESOURCE_DIR, "1. Valid_data.json").toFile();
        List<Map<String, String>> cdrList = MAPPER.readValue(
                file,
                new TypeReference<List<Map<String, String>>>() {
                }
        );

        // Лог: сформировано тело запроса
        String requestBody = MAPPER.writeValueAsString(cdrList);
//        System.out.println("Сформировано тело запроса: " + requestBody);

        // Лог: URL для отправки
        String endpoint = "/processCdrList";
//        System.out.println("Отправлено на URL: " + endpoint);

        // Выполняем запрос и получаем ответ
        Response response = given()
                .contentType(ContentType.JSON)
                .body(cdrList)
                .when()
                .post(endpoint);

        // Лог: получен ответ
//        System.out.println("Получен ответ: " + response.asString());

        // Проверяем статус и тело
        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("", hasSize(cdrList.size()));
    }

    @DisplayName("Невалидный CDR файл")
    @ParameterizedTest(name = "Invalid CDR: {0}")
    @ValueSource(strings = {
            "2. No_exist_flag.json",
            "3. Empty_msisdn.json",
            "4. Uncorrect_msisdn.json",
            "5. Short_number.json",
            "6. Uncorrect_dates.json",
            "7. Empty_field.json",
            "8. Uncorrect_time_format.json",
            "9. Wrong_cdr_type.txt",
            "10. Msisdn_in_two_calls.json",
            "11. Cdr_9_rows.json",
            "12. Wrong_order.json",
            "13. Midnight_no_div.json",
            "14. Too_long_call.json",
            "15. Too_short_call.json",
            "16. Self_calling.json",
    })

    public void processInvalidCdrFile(String fileName) throws Exception {
        File file = Paths.get(CDR_RESOURCE_DIR, fileName).toFile();
        List<Map<String, String>> cdrList = MAPPER.readValue(
                file,
                new TypeReference<List<Map<String, String>>>() {
                }
        );

        given()
                .contentType(ContentType.JSON)
                .body(cdrList)
                .when()
                .post("/processCdrList")
                .then()
                .statusCode(200)
                .body("size()", equalTo(9));
    }

    @Test
    @DisplayName("Тест с пустым CDR")
    void processEmptyCdr() {
        // Отправляем пустой список
        given()
                .contentType(ContentType.JSON)
                .body(List.of())
                .when()
                .post("/processCdrList")
                .then()
                .statusCode(500)
                .body("error", equalTo("Internal Server Error"));
    }
}
