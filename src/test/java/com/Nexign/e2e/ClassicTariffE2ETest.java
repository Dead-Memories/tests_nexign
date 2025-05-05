package com.Nexign.e2e;

import com.Nexign.BaseTest;
import com.Nexign.e2e.Utils.CdrGeneratorUtil;
import com.Nexign.e2e.Utils.DbHelper;
import com.Nexign.e2e.Utils.RestHelper;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E-тест для классического тарифа (tariff_id = 11) по сквозному сценарию:
 * 1) Выбор двух абонентов из базы
 * 2) Генерация кастомного CDR
 * 3) Обработка BRT
 * 4) Проверка наличия звонка в БД
 * 5) Проверка списания баланса инициатора
 */
public class ClassicTariffE2ETest extends BaseTest {
    private static final int TARIFF_ID = 11;
    private static final int TARIFF_TYPE_ID = 1;
    private static String initiator;
    private static String receiver;
    private static float balanceInitBefore;
    private static float balanceRecvBefore;

    @Override
    protected int getPort() {
        return 8081;
    }

    @BeforeAll
    static void setupSubscribers() throws Exception {
        // 1) Отбираем двух абонентов с тарифом 11 из БД BRT
        List<String> list = DbHelper.selectColumn(
                "brt","SELECT msisdn FROM subscriber WHERE tariff_id = " + TARIFF_ID,
                "msisdn"
        );
        assertTrue(list.size() >= 2, "Требуются минимум 2 абонента для теста");
        initiator = list.get(0);
        receiver = list.get(1);

        // 2) Фиксируем их балансы до звонка
        balanceInitBefore = DbHelper.selectFloat(
                "brt","SELECT balance FROM subscriber WHERE msisdn='" + initiator + "'", "balance"
        );
        balanceRecvBefore = DbHelper.selectFloat(
                "brt","SELECT balance FROM subscriber WHERE msisdn='" + receiver + "'", "balance"
        );
    }

//    @Epic("CRM Tests")
//    @Feature("Subscriber CRUD")
//    @Story("Создание абонента")
//    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E: Тарификация исходящего звонка внутри сети длительностью 1 сек")
    @Test
    void classicOutgoingCall() throws Exception {

        // 3) Генерируем CDR: флаг "02" (исходящий), длительность 1 сек
        String cdrJson = CdrGeneratorUtil.generateOneCdr("02", initiator, receiver, 1);

        // 4) Отправляем на обработку BRT
        RestHelper.processCdr(cdrJson);

        // 5) Проверяем, что звонок появился в таблице call
        // Из утилиты DbHelper.existsCall возвращает true/false
        // Берём первый элемент времени из JSON для проверки
        String start = DbHelper.extractFirstField(cdrJson, "startDate");
        String end   = DbHelper.extractFirstField(cdrJson, "endDate");

        assertTrue(DbHelper.existsCall(start, end), "Call record должна быть в BRT DB");

        // 6) Проверяем списание баланса инициатора: 1 сек -> округление до минуты (1)
        float balanceInitAfter = DbHelper.selectFloat(
                "brt","SELECT balance FROM subscriber WHERE msisdn='" + initiator + "'", "balance"
        );
        float ratePerMin = DbHelper.selectFloat(
                "hrs",
                "SELECT initiating_external_call_cost FROM tariff_parameter WHERE tariff_type_id = " + TARIFF_TYPE_ID,
                "initiating_external_call_cost"
        );

        float expectedDeduction = ratePerMin;
        assertEquals(
                balanceInitBefore - expectedDeduction,
                balanceInitAfter,
                0.01,
                "Баланс инициатора должен быть уменьшен на " + ratePerMin + " денег"
        );

        // 7) У получателя баланс не изменился
        float balanceRecvAfter = DbHelper.selectFloat(
                "brt","SELECT balance FROM subscriber WHERE msisdn='" + receiver + "'", "balance"
        );
        assertEquals(balanceRecvBefore, balanceRecvAfter, 0.01,
                "Баланс принимающего вызов не должен меняться");
    }
}
