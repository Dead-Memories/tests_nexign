package com.Nexign.e2e.Utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Утилита для работы с базой Postgres в E2E-тестах
 */
public class DbHelper {

    /**
     * Возвращает соединение с любой БД
     */
    public static Connection getDbConnection(String service) throws Exception {
        Class.forName("org.postgresql.Driver");
        String url;
        switch (service) {
            case "brt":
                url = System.getenv("BRT_DB_URL");
                break;
            case "hrs":
                url = System.getenv("HRS_DB_URL");
                break;
            default:
                throw new IllegalArgumentException("Неизвестный сервис: " + service);
        }
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");
        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("Креды для БД не установлены в переменных окружения");
        }
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Выполняет SELECT и возвращает все значения одного столбца из указанной базы.
     */
    public static List<String> selectColumn(String service, String sql, String column) throws Exception {
        try (Connection conn = getDbConnection(service);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<String> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getString(column));
            }
            return result;
        }
    }

    /**
     * Выполняет SELECT на указанном сервисе и возвращает единственное значение float из указанного столбца.
     */
    public static float selectFloat(String service, String sql, String column) throws Exception {
        try (Connection conn = getDbConnection(service);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getFloat(column);
            } else {
                throw new SQLException("Запрос не дал результатов: " + sql);
            }
        }
    }

    /**
     * Проверяет, что запись звонка существует по startCall и endCall.
     */
    public static boolean existsCall(String start, String end) throws Exception {
        String sql = String.format(
                "SELECT 1 FROM call WHERE start_call='%s' AND end_call='%s' LIMIT 1", start, end
        );
        try (Connection conn = getDbConnection("brt");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }

    /**
     * Извлекает значение первого поля из JSON-массива CDR-записи.
     */
    public static String extractFirstField(String jsonArray, String fieldName) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> list = mapper.readValue(
                jsonArray, new TypeReference<List<Map<String, Object>>>() {}
        );
        if (list.isEmpty()) {
            throw new IllegalArgumentException("JSON array пуст");
        }
        Object val = list.get(0).get(fieldName);
        return val != null ? val.toString() : null;
    }

    public static int selectInt(String service, String sql, String column) throws Exception {
        try (Connection conn = getDbConnection(service);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(column);
            } else {
                throw new RuntimeException("No rows returned for query: " + sql);
            }
        }
    }

}
