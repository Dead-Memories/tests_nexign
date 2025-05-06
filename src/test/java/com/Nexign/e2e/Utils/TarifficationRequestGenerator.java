package com.Nexign.e2e.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Утилита для генерации случайных тел запросов и расчёта ожидаемых значений
 * для POST /tarifficateCall
 */
public class TarifficationRequestGenerator {
    private static final Random RANDOM = new Random();

    public static class RequestResult {
        public final Map<String, Object> body;
        public final float expectedBalanceChange;
        public final int expectedTariffBalanceChange;

        public RequestResult(Map<String, Object> body, float expectedBalanceChange, int expectedTariffBalanceChange) {
            this.body = body;
            this.expectedBalanceChange = expectedBalanceChange;
            this.expectedTariffBalanceChange = expectedTariffBalanceChange;
        }
    }

    public static RequestResult generate() {
        // Параметры
        int minutes = RANDOM.nextInt(11); // 0..10
        int callType = RANDOM.nextBoolean() ? 1 : 2;
        int isRomashka = RANDOM.nextBoolean() ? 1 : 0;
        int tariffId = RANDOM.nextBoolean() ? 12 : 11;
        int tariffBalance = RANDOM.nextInt(50) + 1; // 1..50
        float balance = RANDOM.nextFloat() * 99 + 1; // 1..100

        float balanceChange = 0f;
        int tariffBalanceChange = 0;

        // Логика расчёта
        if (callType == 2) {
            // Бесплатный звонок
            balanceChange = 0f;
            tariffBalanceChange = 0;
        } else {
            // Платный звонок
            if (tariffId == 12) {
                // Помесячный
                if (minutes <= tariffBalance) {
                    // Минут достаточно
                    tariffBalanceChange = minutes;
                    balanceChange = 0f;
                } else {
                    // Недостаточно минут
                    tariffBalanceChange = tariffBalance;
                    int extra = minutes - tariffBalance;
                    // Списание по классике
                    if (isRomashka == 1) {
                        balanceChange = extra * 1.5f;
                    } else {
                        balanceChange = extra * 2.5f;
                    }
                }
            } else {
                // Классический тариф
                // Все минуты оплачиваются по классике
                if (isRomashka == 1) {
                    balanceChange = minutes * 1.5f;
                } else {
                    balanceChange = minutes * 2.5f;
                }
                tariffBalanceChange = 0;
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("minutes", minutes);
        body.put("callType", callType);
        body.put("isRomashkaCall", isRomashka);
        body.put("tariffId", tariffId);
        body.put("tariffBalance", tariffBalance);
        body.put("balance", balance);

        return new RequestResult(body, balanceChange, tariffBalanceChange);
    }
}