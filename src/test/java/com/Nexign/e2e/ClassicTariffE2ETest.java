package com.Nexign.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassicTariffE2ETest {
    private static final String MSISDN = "79991234567";

    // Вспомогательный метод для подключения к базам Postgres
    private Connection getDbConnection(String service) throws Exception {
        Class.forName("org.postgresql.Driver");
        String url;
        switch (service) {
            case "brt":
                url = "jdbc:postgresql://127.0.0.1:5430/brt_db";
                break;
            case "hrs":
                url = "jdbc:postgresql://localhost:5440/hrs_db";
                break;
            default:
                throw new IllegalArgumentException("Unknown service: " + service);
        }
        String user = "user";
        String pass = "password";
        return DriverManager.getConnection(url, user, pass);
    }

    @Test
    @DisplayName("Тест соединения с БД")
    void fullClassicFlow() throws Exception {
        // 1) Создать абонента с тарифом «Классический» (POST /manager/subscriber/add)
        // 2) Пополнить баланс (PUT /subscriber/{msisdn}/changebalance)
        // 3) Сгенерить и обработать CDR через BRT (/processCdrList)
        //    – убедиться, что баланс уменьшился на minutes * rate
        // 4) Проверить баланс (GET /subscriber/{msisdn}/getbalance)
        // 5) Вызвать HRS /tarifficateCall для одного разговора и сверить балансChange
        // 6) Удалить абонента (DELETE /manager/subscriber/{msisdn}/delete)

        try (Connection conn = getDbConnection("brt");
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT balance FROM subscriber WHERE msisdn='" + MSISDN + "'"
            );
            if (rs.next()) {
                assertEquals(165.5F, rs.getFloat("balance"),
                        "CRM DB balance must match API");
            }
        }
    }
}
