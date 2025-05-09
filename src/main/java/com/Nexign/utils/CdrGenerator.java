package com.Nexign.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Утилита для генерации JSON-файлов с CDR. Все файлы сохраняются в отдельную папку "TestResources":
 * 1) cdr_valid.json — 10 валидных записей
 * 2) cdr_<scenario>.json — 9 валидных + 1 запись с конкретным негативным кейсом
 */
public class CdrGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Random RANDOM = new Random();
    private static final String OUTPUT_DIR = "/Users/olya/IdeaProjects/tests_nexign/src/main/java/com/Nexign/TestResources";


    public static void main(String[] args) throws Exception {
        File dir = new File(OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        // 1. Генерируем файл с 10 валидными CDR
        List<Record> rawValid = new ArrayList<>();
        for (int i = 0; i < 10; i++) rawValid.add(createRecord(generateValidCdr()));
        List<Record> validProcessed = rawValid.stream()
                .flatMap(CdrGenerator::splitIfCrossMidnight)
                .sorted(Comparator.comparing(r -> r.start))
                .collect(Collectors.toList());
        writeJson(mapper, new File(dir, "cdr_valid.json"), validProcessed);
        System.out.println("Generated cdr_valid.json with 10 valid records");

        // 2. Генерируем файлы с негативными кейсами: каждый файл содержит 9 валидных + 1 негативный кейс
        Map<String, Supplier<Map<String, String>>> negativeCases = new LinkedHashMap<>();
        negativeCases.put("invalid_dates", CdrGenerator::generateInvalidDates);
        negativeCases.put("nonexistent_operator", CdrGenerator::generateNonexistentOperator);
        negativeCases.put("invalid_msisdn", CdrGenerator::generateInvalidMsisdn);
        negativeCases.put("bad_flag", CdrGenerator::generateBadFlag);
        negativeCases.put("missing_field", CdrGenerator::generateMissingField);

        for (Map.Entry<String, Supplier<Map<String, String>>> entry : negativeCases.entrySet()) {
            String caseName = entry.getKey();
            Supplier<Map<String, String>> generator = entry.getValue();
            List<Map<String, String>> list = new ArrayList<>();
            // 9 валидных
            for (int i = 0; i < 9; i++) {
                list.add(generateValidCdr());
            }
            // 1 негативная запись
            list.add(generator.get());

            String fileName = "cdr_" + caseName + ".json";
            File outFile = new File(dir, fileName);
            mapper.writeValue(outFile, list);
            System.out.println("Generated " + fileName + " with 9 valid + 1 " + caseName);
        }
    }

    private static void writeJson(ObjectMapper mapper, File file, List<Record> records) throws Exception {
        List<Map<String,String>> list = records.stream().map(Record::toMap).collect(Collectors.toList());
        mapper.writeValue(file, list);
        System.out.println("Generated " + file.getAbsolutePath() + " with " + records.size() + " records");
    }

    // Для удобства структуры
    private static class Record {
        LocalDateTime start;
        LocalDateTime end;
        String flag, initiator, receiver;
        Record(LocalDateTime s, LocalDateTime e, String f, String i, String r) {
            start=s; end=e; flag=f; initiator=i; receiver=r;
        }
        Map<String,String> toMap() {
            return Map.of(
                    "flag", flag,
                    "initiator", initiator,
                    "receiver", receiver,
                    "startDate", start.format(FORMATTER),
                    "endDate",   end.format(FORMATTER)
            );
        }
    }

    private static Record createRecord(Map<String,String> data) {
        LocalDateTime s = LocalDateTime.parse(data.get("startDate"), FORMATTER);
        LocalDateTime e = LocalDateTime.parse(data.get("endDate"), FORMATTER);
        return new Record(s, e, data.get("flag"), data.get("initiator"), data.get("receiver"));
    }

    private static java.util.stream.Stream<Record> splitIfCrossMidnight(Record r) {
        if (r.start.toLocalDate().equals(r.end.toLocalDate())) {
            return java.util.stream.Stream.of(r);
        } else {
            LocalDateTime endOfDay = LocalDateTime.of(r.start.toLocalDate(), LocalTime.MAX);
            LocalDateTime startNext = LocalDateTime.of(r.end.toLocalDate(), LocalTime.MIN);
            Record first = new Record(r.start, endOfDay, r.flag, r.initiator, r.receiver);
            Record second = new Record(startNext, r.end, r.flag, r.initiator, r.receiver);
            return java.util.stream.Stream.of(first, second);
        }
    }


    // Генерация валидной CDR-записи
    private static Map<String, String> generateValidCdr() {
        LocalDateTime start = randomDateTime();
        LocalDateTime end = start.plusSeconds(30 + RANDOM.nextInt(3600));
        return Map.of(
                "flag", randomFlag(),
                "initiator", randomPhone(),
                "receiver", randomPhone(),
                "startDate", start.format(FORMATTER),
                "endDate", end.format(FORMATTER)
        );
    }

    // Негативные генераторы
    private static Map<String, String> generateInvalidDates() {
        LocalDateTime now = LocalDateTime.now();
        return Map.of(
                "flag", "01",
                "initiator", randomPhone(),
                "receiver", randomPhone(),
                "startDate", now.format(FORMATTER),
                "endDate", now.minusHours(1).format(FORMATTER)
        );
    }

    private static Map<String, String> generateNonexistentOperator() {
        return Map.of(
                "flag", "01",
                "initiator", "711234567890", // префикс 71 нет в операторах
                "receiver", randomPhone(),
                "startDate", nowStr(),
                "endDate", nowPlusMin(5)
        );
    }

    private static Map<String, String> generateInvalidMsisdn() {
        return Map.of(
                "flag", "01",
                "initiator", randomPhone(),
                "receiver", "79890", // слишком короткий
                "startDate", nowStr(),
                "endDate", nowPlusMin(3)
        );
    }

    private static Map<String, String> generateBadFlag() {
        return Map.of(
                "flag", "99", // неизвестный флаг
                "initiator", randomPhone(),
                "receiver", randomPhone(),
                "startDate", nowStr(),
                "endDate", nowPlusMin(1)
        );
    }

    private static Map<String, String> generateMissingField() {
        Map<String, String> map = new HashMap<>();
        map.put("flag", "01");
        map.put("initiator", randomPhone());
        // пропускаем receiver
        map.put("startDate", nowStr());
        map.put("endDate", nowPlusMin(2));
        return map;
    }

    // Вспомогательные методы
    private static String randomFlag() {
        return RANDOM.nextBoolean() ? "01" : "02";
    }

    private static String randomPhone() {
        // Все номера начинаются с '7' + 10 цифр
        StringBuilder sb = new StringBuilder("7");
        for (int i = 0; i < 10; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private static LocalDateTime randomDateTime() {
        int year = 2024;
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28);
        int hour = RANDOM.nextInt(24);
        int min = RANDOM.nextInt(60);
        int sec = RANDOM.nextInt(60);
        return LocalDateTime.of(year, month, day, hour, min, sec);
    }

    private static String nowStr() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private static String nowPlusMin(int minutes) {
        return LocalDateTime.now().plusMinutes(minutes).format(FORMATTER);
    }
}
