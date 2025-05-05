package com.Nexign.e2e.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Утилита для генерации CDR-файлов c 1 записью или 10 записями
 */
public class CdrGeneratorUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Random RANDOM = new Random();

    /**
     * Генерирует JSON-массив из 10 CDR-записей:
     * - 1 кастомная запись с переданными параметрами
     * - 9 рандомных записей
     */
    public static String generateCdrFile(
            String flag,
            String initiator,
            String receiver,
            int durationSec
    ) throws Exception {
        List<Map<String, String>> cdrList = new ArrayList<>();

        // 1 кастомная запись
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(durationSec);
        Map<String, String> custom = new LinkedHashMap<>();
        custom.put("flag", flag);
        custom.put("initiator", initiator);
        custom.put("receiver", receiver);
        custom.put("startDate", now.format(FORMATTER));
        custom.put("endDate", end.format(FORMATTER));
        cdrList.add(custom);

        // 9 случайных записей
        for (int i = 0; i < 9; i++) {
            cdrList.add(generateRandomCdr());
        }

        return MAPPER.writeValueAsString(cdrList);
    }

    public static String generateOneCdr(
            String flag,
            String initiator,
            String receiver,
            int durationSec
    ) throws Exception {

        List<Map<String, String>> cdrList = new ArrayList<>();

        // 1 кастомная запись
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(durationSec);
        Map<String, String> custom = new LinkedHashMap<>();
        custom.put("flag", flag);
        custom.put("initiator", initiator);
        custom.put("receiver", receiver);
        custom.put("startDate", now.format(FORMATTER));
        custom.put("endDate", end.format(FORMATTER));
        cdrList.add(custom);

        return MAPPER.writeValueAsString(cdrList);
    }


    // Генерация одной случайной CDR-записи
    private static Map<String, String> generateRandomCdr() {
        LocalDateTime start = randomDateTime();
        LocalDateTime end = start.plusSeconds(30 + RANDOM.nextInt(3600));
        return Map.of(
                "flag", RANDOM.nextBoolean() ? "01" : "02",
                "initiator", randomPhone(),
                "receiver", randomPhone(),
                "startDate", start.format(FORMATTER),
                "endDate", end.format(FORMATTER)
        );
    }

    // Случайная дата/время в 2024 году
    private static LocalDateTime randomDateTime() {
        int year = 2024;
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28);
        int hour = RANDOM.nextInt(24);
        int min = RANDOM.nextInt(60);
        int sec = RANDOM.nextInt(60);
        return LocalDateTime.of(year, month, day, hour, min, sec);
    }

    // Случайный российский номер, начинающийся с 79
    private static String randomPhone() {
        StringBuilder sb = new StringBuilder("79");
        for (int i = 0; i < 9; i++) {
            sb.append(RANDOM.nextInt(9));
        }
        return sb.toString();
    }
}
