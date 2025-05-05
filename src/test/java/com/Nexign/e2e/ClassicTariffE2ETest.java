package com.Nexign.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassicTariffE2ETest {
    private static final String MSISDN = "79991234567";

    // Вспомогательный метод для подключения к базам Postgres
    private Connection getDbConnection(String service) throws Exception {
        String urlKey = service.toUpperCase() + "_DB_URL";
        String url = System.getenv(urlKey);
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");
        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("Не заданы переменные окружения для БД");
        }
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
                    "SELECT * FROM public.subscriber"
            );
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    String name = meta.getColumnName(i);
                    Object val  = rs.getObject(i);
                    row.append(name).append("=").append(val).append("; ");
                }
                System.out.println(row.toString());
            }
//            if (rs.next()) {
//                assertEquals(165.5F, rs.getFloat("balance"),
//                        "CRM DB balance must match API");
//            }
        }
    }
}
