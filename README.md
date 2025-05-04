## 2. Автотесты на валидацию CDR сервисом BRT

Перед написанием автотеста валидация CDR сервисом BRT была протестирована вручную. Результаты проверки лежат в [документе](https://docs.google.com/spreadsheets/d/1Z3PP9ipYX9FlLFWhs0BKGFT_9WMVagtTPuESBTi5ByA/edit?usp=sharing), по результатам проверки заключено, что сервис не всегда корректно отрабатывает при негативных сценариях, так как при разработке предполагалось, что BRT уверен в корректности данных, которые получает от CDR. Заводить баг и отправлять на доработку разработчику учитывая сроки и время разработки не считаю целесообразным. 

Для "подкладывания" искусственно сгенерированных разработчиком реализована ручка /processCdrList, за что отдельное спасибо. 

Автотесты лежат в /src/test/java/com/Nexign/ProcessCdrListTest. В тестовом сценарии реализовано 3 автотеста: обработка валидного CDR, обработка 15-ти невалидных CDR и обработка пустого CDR.  

План тестирования для написания автотеста:
- Предусловия: 
Сгенерированные файлы для тестирования лежат в папке [Файлы для тестирования BRT](https://drive.google.com/drive/folders/17oAoHgWyYd8rFhmL_HNaUA-M7aJ1ccWh)
В проекте тестовые данные лежат в /src/test/java/com/Nexign/TestResources.

- Сценарий:
  1. Считать файл с тестовыми данными из репозитория
  2. Сформировать тело запроса с содержимым из файла json
  3. Отправить сгенерированный CDR в BRT запросом POST http://localhost:8081/processCdrList
  4. Сравнивать код ответа и тело ответа с ожидаемым:
     а) Для валидного файла код ответа 200 ОК, тело ответа в формате
     [
    {
        "tariffBalanceChange": -50,
        "balanceChange": -268.5
    },
    {
        "tariffBalanceChange": 0,
        "balanceChange": -785.0
    }, .....
     b) Для невалидного файла код ответа 400 Bad Request / сообщение об ошибке
     с) Для пустого файла код ответа 500 Internal Server Error

Результат тестирования на данном этапе выглядит так: 

<img src="Test_results.png" width="400" alt="Скриншот результата" />


## 4. Автоматизация создания тестовых CDR

В папке src/main/java/com/Nexign/utils/ лежит файл CdrGenerator.java. Предназначен для автоматической генерации случайных CDR-файлов. В результате работы алгоритма получаем следующие файлы:

1. cdr_valid.json – файл с валидными 10 записями CDR 
2. cdr_bad_flag.json – файл с одной записью с некорректным флагом
3. cdr_invalid_dates.json – файл с одной записью с некорректной датой (дата начала позже даты конца)
4. cdr_invalid_msisdn.json – файл с некорректным форматом номера телефона 
4. cdr_missing_field.json – файл с пропущенным полем
5. cdr_nonexistent_operator.json – несуществующий оператор (некорректный номер)

Код гибкий, позволяет добавлять любые другие типы некорректных проверок для генерации данных. Исходные данные и требования лежат по ссылке https://docs.google.com/spreadsheets/d/1Z3PP9ipYX9FlLFWhs0BKGFT_9WMVagtTPuESBTi5ByA/edit?usp=sharing

Записи в CDR файле расположены в хронологическом порядке, записи пересекающие рубеж суток 00:00 разделены на две записи.

В данный момент генерация ведется по рандомным msisdn, но можно взять те, что лежат у нас в базе и генерировать из мапы реальных наших абонентов.


## 1. Автотесты на API сервиса с использованием RestAssured

Опишем, какие эндпоинты у нас есть для дальнейшего их покрытия. 

1. Коммутатор
   - POST http://localhost:8080/generate
   - DELETE http://localhost:8080/truncate
2. BRT
   - POST http://localhost:8081/processCdrList
3. HRS
   - POST http://localhost:8082/tarifficateCall
   - GET http://localhost:8082/monthTariffication/11
4. CRM
   - POST http://localhost:8083/manager/subscriber/add
   - GET http://localhost:8083/manager/subscriber/{msisdn}/fullinfo
   - PATCH http://localhost:8083/manager/subscriber/{msisdn}/update
   - DELETE http://localhost:8083/manager/subscriber/{msisdn}/delete
   - GET http://localhost:8083/subscriber/{msisdn}/getbalance
   - GET http://localhost:8083/manager/subscriber/{msisdn}/gettariff
   - PUT http://localhost:8083/manager/subscriber/{{msisdn}}/changetariff
   - PUT http://localhost:8083/subscriber/{msisdn}/changebalance

| Сервис      | Тип запроса | Запрос                                         | Тело запроса            | Код ответа   | Ответ                 | Покрытие                    |
|-------------|-------------|------------------------------------------------|-------------------------|--------------|-----------------------|---------------------------  |
| Коммутатор  | POST        | `/generate`                                    | —                       | 200 ОК       | {num} rows genetated  | Отправить запрос, проверить ответ      |
| Коммутатор  | DELETE      | `/truncate`                                    | —                       | 204          | -                     |(?)                          |
| BRT         | POST        | `/processCdrList`                              | JSON-массив CDR         | 200 ОК       | -                     | Покрыт в задании 2          |
| HRS         | POST        | `/tarifficateCall`                             | {"minutes": 10, "callType": 1, "isRomashkaCall": 1, "tariffId": 12, "tariffBalance": 6, "balance": 0.0}        | 200 ОК       | -                     | Покрыть основные сценарии для ТП              |
| HRS         | GET         | `/monthTariffication/11`                       | —                       | 200 ОК       | tarif info            | Покрыть       |
| CRM         | POST        | `/manager/subscriber/add`                      | JSON SubInfo            | 201          | -                     |               |
| CRM         | GET         | `/manager/subscriber/{msisdn}/fullinfo`        | —                       | 200 ОК       | -                     |               |
| CRM         | PATCH       | `/manager/subscriber/{msisdn}/update`          | JSON SubInfo            | 200 ОК       | -                     |               |
| CRM         | DELETE      | `/manager/subscriber/{msisdn}/delete`          | —                       | 204          | Deleted               |               |
| CRM         | GET         | `/subscriber/{msisdn}/getbalance`              | —                       | 200 ОК       | -                     |               |
| CRM         | GET         | `/manager/subscriber/{msisdn}/gettariff`       | —                       | 200 ОК       | -                     |               |
| CRM         | PUT         | `/manager/subscriber/{{msisdn}}/changetariff`  | JSON NewTariffInfo      | 200 ОК       | -                     |               |
| CRM         | PUT         | `/subscriber/{msisdn}/changebalance`           | JSON BalanceTopUp       | 200 ОК       | -                     |               |


#### Полученные автотесты на API по заданию: 
1) Коммутатор
   
  а) Лежат в файле CommutatorApiTest.
  
  b) Содержат 3 теста и последующую проверку ответа сервера: вызов API \generate, вызов API \truncate и повторный вызов API \generate при уже сгенерированных данных.
  
  c) Тесты прошли успешно, результат на скриншоте ниже.

2) BRT
   
   а) Сервис содержит одну API и проверяется в рамках задания 2.
5) HRS
  а) 
6) CRM
  а)
  b)
  c) Успешность тестов пока не проверена, так как API CRM еще не реализован.

## 3. Автотесты на e2e процесс
