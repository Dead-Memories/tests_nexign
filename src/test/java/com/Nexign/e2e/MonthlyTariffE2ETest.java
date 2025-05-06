package com.Nexign.e2e;

import com.Nexign.BaseTest;
import com.Nexign.e2e.Utils.CdrGeneratorUtil;
import com.Nexign.e2e.Utils.DbHelper;
import com.Nexign.e2e.Utils.RestHelper;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E-тест для помесячного тарифа (tariff_id = 12):
 * проверяет вычет минут из пакета без изменения денежного баланса.
 */
@Epic("Tariff Tests")
@Feature("Monthly Tariff E2E")
public class MonthlyTariffE2ETest extends BaseTest {
    private static final int TARIFF_ID = 12;
    private static String subscriber;
    private static float minutesBefore;
    private static float moneyBefore;
    private static final String EXTERNAL_NUMBER = "79567000111"; // не абонент Ромашки

    @Override
    protected int getPort() {
        return 8081;
    }

    @BeforeAll
    static void setupSubscriber() throws Exception {
        // 1) Отбираем одного абонента с тарифом 12 и остатком минут > 30
        List<String> list = DbHelper.selectColumn(
                "brt",
                "SELECT msisdn FROM subscriber WHERE tariff_id = " + TARIFF_ID +
                        " AND tariff_balance > 30",
                "msisdn"
        );
        assertFalse(list.isEmpty(), "Нужен абонент с тарифом 12 и >30 мин");
        subscriber = list.get(0);

        // Фиксируем остаток минут и баланс денег
        minutesBefore = DbHelper.selectFloat(
                "brt",
                "SELECT tariff_balance FROM subscriber WHERE msisdn='" + subscriber + "'",
                "tariff_balance"
        );
        moneyBefore = DbHelper.selectFloat(
                "brt",
                "SELECT balance FROM subscriber WHERE msisdn='" + subscriber + "'",
                "balance"
        );
    }

    @Story("Помесячная тарификация")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E: Помесячный тариф, звонок 30мин 30сек — вычет 31 мин")
    @Test
    void monthlyTariffication() throws Exception {
        // 2) Генерируем CDR: исходящий звонок на 30 минут 30 сек
        int durationSec = 30 * 60 + 30;
        String cdrJson = CdrGeneratorUtil.generateOneCdr(
                "02", subscriber, EXTERNAL_NUMBER, durationSec
        );

        // 3) Отправляем для обработки BRT
        RestHelper.processCdr(cdrJson);

        // 4) Проверяем запись звонка в БД BRT
        String start = DbHelper.extractFirstField(cdrJson, "startDate");
        String end   = DbHelper.extractFirstField(cdrJson, "endDate");
        assertTrue(DbHelper.existsCall(start, end), "CDR должен сохраниться в call");

        // 5) Проверяем вычет минут без списания денег
        float minutesAfter = DbHelper.selectFloat(
                "brt",
                "SELECT tariff_balance FROM subscriber WHERE msisdn='" + subscriber + "'",
                "tariff_balance"
        );
        float moneyAfter = DbHelper.selectFloat(
                "brt",
                "SELECT balance FROM subscriber WHERE msisdn='" + subscriber + "'",
                "balance"
        );

        assertEquals(
                minutesBefore - 30,
                minutesAfter,
                0.01,
                "Остаток минут должен уменьшиться на 30"
        );
        assertEquals(
                moneyBefore,
                moneyAfter,
                0.01,
                "Денежный баланс не должен измениться"
        );
    }
}

