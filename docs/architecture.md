# Архитектура проекта

## Общие принципы

1. **Язык**: Clojure 1.12+
2. **Стиль**: Идиоматичный Clojure
3. **Документация**: Изменения структуры, конфигурации и поведения должны сопровождаться обновлением документации.

## Именование неймспейсов

- `meteomax.*` - стартовый код приложения и общая конфигурация
- `meteomax.app.*` - бизнес-логика, HTTP интеграции, webhook, форматирование
- `meteomax.db.*` - доступ к PostgreSQL
- `meteomax.metrics.*` - метрики и экспорт
- `meteomax.lib.*` - общие утилиты

## Структура кода

```text
src/meteomax/
  main.clj
  config.clj
  app/
    command.clj
    dispatch.clj
    fmt.clj
    maxapi.clj
    meteo_api.clj
    sender.clj
    subs.clj
    webhook.clj
  db/
    pg.clj
    users.clj
    subscriptions.clj
  lib/
    envvar.clj
    random.clj
  metrics/
    export.clj
    reg.clj
```

## Управление жизненным циклом

Проект использует **mount**.

Базовые состояния:

- `meteomax.db.pg/conn` - пул подключений к PostgreSQL
- `meteomax.main/sender-proc` - планировщик рассылок
- `meteomax.main/metrics-endpoint` - HTTP сервер метрик
- `meteomax.app.webhook/webhook-secret` - runtime secret для MAX webhook
- `meteomax.app.webhook/webhook-endpoint` - HTTP webhook сервер
- `meteomax.app.webhook/webhook-subscription` - регистрация webhook в MAX API

Типичный запуск:

```clojure
(require '[mount.core :as mount]
         '[meteomax.config :as config])

(defn -main []
  (mount/start-with-args (config/make-config)))
```

## Конфигурация

Конфигурация валидируется через **malli**.

Ключевые параметры:

- `:max-api-token`
- `:database-url`
- `:meteo-api-url`
- `:meteo-api-auth`
- `:meteo-api-timeout`
- `:metrics-bind`
- `:metrics-port`
- `:webhook-bind`
- `:webhook-port`
- `:webhook-url`
- `:webhook-path`
- `:timezone`
- `:zone-id`

`WEBHOOK_PATH` может быть задан явно. Если он не задан, путь вычисляется из `WEBHOOK_URL`. `:zone-id` вычисляется из `:timezone`.

## Работа с БД

PostgreSQL доступен через **pg2** (`pg.core`).

Пул создаётся в `meteomax.db.pg`:

```clojure
(require '[pg.core :as pg])

(defn make-pool [url]
  (pg/pool url))
```

### DB-модули

- `meteomax.db.users` - пользователи, геопозиция, избранные станции
- `meteomax.db.subscriptions` - подписки на погодные уведомления

Текущий слой БД использует параметризованные SQL-строки через `pg/execute`, а не HoneySQL.

Пример:

```clojure
(pg/execute db
            "select * from subscriptions where chat_id = $1"
            {:params [chat-id]})
```

### Предполагаемая схема таблиц

`users`

- `chat_id`
- `username`
- `first_name`
- `latitude`
- `longitude`
- `favorites`

`subscriptions`

- `id`
- `chat_id`
- `station_name`
- `time_str`
- `days_of_week`
- `active`

### Миграции

Документационно проект ориентирован на миграции в `resources/migrations/`, но в текущем репозитории migration-файлы отсутствуют. Если схема меняется, миграции нужно добавить вместе с кодом.

## HTTP интеграции

### Meteo API

`meteomax.app.meteo-api` использует **http-kit** как HTTP клиент и **core.memoize** для TTL-кэша.

- `get-active-stations`
- `get-station-info`

### MAX Bot API

`meteomax.app.maxapi` инкапсулирует вызовы к `https://platform-api.max.ru`.

Основные операции:

- `get-updates`
- `get-subscriptions`
- `set-webhook`
- `send-message`
- `send-location`
- `answer-callback`
- `get-me`
- `upload-file`

## Webhook

Webhook реализован в `meteomax.app.webhook`.

Основные обязанности модуля:

- поднять HTTP endpoint на `http-kit`
- сгенерировать `webhook-secret` через `SecureRandom`
- зарегистрировать webhook в MAX API через `POST /subscriptions`
- валидировать заголовок `X-Max-Bot-Api-Secret`
- распарсить входящий JSON `Update`
- маршрутизировать поддержанные `update_type`

Сейчас обрабатываются:

- `message_created`
- `message_callback`
- `bot_started`

Остальные события логируются и игнорируются.

## Командный слой

`meteomax.app.command` содержит пользовательские обработчики:

- `/start`
- `/help`
- `/near`
- `/active`
- `/favs`
- `/info`
- `/subs`
- `/sub`

`/sub` в текущей реализации создаёт подписку по строке формата:

```text
/sub <station> <HH:MM> <days>
```

Парсинг этой команды вынесен в `meteomax.app.subs/parse-subscription-input`.

## Планировщик

`meteomax.app.sender` использует **chime** и раз в минуту проверяет активные подписки из БД.

Пайплайн:

1. Прочитать активные подписки через `meteomax.db.subscriptions/get-all-active-subs`
2. Проверить совпадение дня недели и времени
3. Получить актуальную погоду через `meteomax.app.meteo-api`
4. Отправить сообщение через `meteomax.app.maxapi/send-message`

## Метрики

`meteomax.metrics.reg` описывает Prometheus-метрики через **iapetos**.

`meteomax.metrics.export` поднимает endpoint `/metrics` через **http-kit**.

## Тестирование и проверка качества

После изменений в коде использовать:

```bash
clj-kondo --lint src test
make check-reflect
clj -M:test
```
