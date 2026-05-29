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
    fmt.clj
    maxapi.clj
    sender.clj
    webhook.clj
  db/
    pg.clj
    users.clj
    subs.clj
  meteo_data/
    core.clj
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
- `meteomax.db.subs` - подписки на погодные уведомления

Текущий слой БД использует параметризованные SQL-строки через `pg/execute`, а не HoneySQL.

Пример:

```clojure
(pg/execute db
            "select * from subs where user_id = $1"
            {:params [user-id]})
```

### Схема таблиц

#### users

| Колонка            | Тип                | Описание                                        |
|--------------------|--------------------|-------------------------------------------------|
| `user_id`          | `TEXT PRIMARY KEY` | Идентификатор пользователя в MAX Bot API        |
| `chat_id`          | `TEXT`             | Идентификатор приватного чата с ботом           |
| `created_at`       | `TIMESTAMPTZ`      | Дата первого обращения                          |
| `updated_at`       | `TIMESTAMPTZ`      | Дата последнего обновления профиля              |
| `username`         | `TEXT`             | Логин пользователя (nullable)                   |
| `last_send_at`     | `TIMESTAMPTZ`      | Время последней отправки уведомления (nullable) |
| `last_send_status` | `TEXT`             | Статус последней отправки (nullable)            |
| `favs`             | `TEXT[]`           | Массив названий избранных станций               |
| `location`         | `JSONB[]`          | История геопозиций                              |
| `info`             | `JSONB`            | Дополнительные данные пользователя              |

#### subs

| Колонка        | Тип                  | Описание                                              |
|----------------|----------------------|-------------------------------------------------------|
| `id`           | `SERIAL PRIMARY KEY` | Авто-инкремент                                        |
| `user_id`      | `TEXT NOT NULL`      | Идентификатор пользователя (без FK-ограничения)       |
| `chat_id`      | `TEXT NOT NULL`      | Чат, в который отправляется уведомление               |
| `station_name` | `TEXT NOT NULL`      | Идентификатор метеостанции                            |
| `time_str`     | `TEXT NOT NULL`      | Время уведомления в формате `HH:MM`                   |
| `days_of_week` | `TEXT NOT NULL`      | Строка цифр — дни недели (пн=1 … вс=7). Например: `"135"` = пн, ср, пт; `"1234567"` = каждый день |
| `active`       | `BOOLEAN`            | Подписка активна (DEFAULT true)                       |
| `created_at`   | `TIMESTAMPTZ`        | Дата создания подписки                                |

### Миграции

Миграции хранятся в `resources/migrations/`. При изменении схемы миграции добавляются вместе с кодом.

## HTTP интеграции

### Meteo API

`meteomax.meteo-data.core` использует **http-kit** как HTTP клиент и **core.memoize** для TTL-кэша.

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
- `/favs`
- `/subs`

Текстовые сообщения длиной 3+ символа обрабатываются как поисковый запрос по станциям.
Геопозиция возвращает 5 ближайших активных станций.

## Планировщик

`meteomax.app.sender` использует **chime** и раз в минуту проверяет активные подписки из БД.

Пайплайн:

1. Прочитать активные подписки через `meteomax.db.subs/get-all-active-subs`
2. Проверить совпадение дня недели и времени
3. Получить актуальную погоду через `meteomax.meteo-data.core`
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
