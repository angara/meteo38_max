# Архитектура проекта

## Общие принципы

1. **Язык**: Clojure 1.12+
2. **Стиль**: Идиоматичный Clojure
3. **Документация**: Изменения структуры, конфигурации и поведения должны сопровождаться обновлением документации.

## Именование неймспейсов

- `meteomax.*` — стартовый код приложения и общая конфигурация
- `meteomax.app.*` — бизнес-логика, HTTP интеграции, webhook, форматирование
- `meteomax.db.*` — доступ к PostgreSQL
- `meteomax.metrics.*` — метрики и экспорт
- `meteomax.lib.*` — общие утилиты

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

Состояния:

- `meteomax.db.pg/conn` — пул подключений к PostgreSQL
- `meteomax.main/metrics-endpoint` — HTTP сервер метрик
- `meteomax.main/sender-proc` — планировщик рассылок
- `meteomax.main/bot-info` — информация о боте (`GET /me`)
- `meteomax.main/bot-commands-registered` — регистрация команд бота (`PATCH /me`)
- `meteomax.app.webhook/webhook-secret` — runtime secret для MAX webhook
- `meteomax.app.webhook/webhook-endpoint` — HTTP webhook сервер
- `meteomax.app.webhook/webhook-subscription` — регистрация webhook в MAX API

## Работа с БД

PostgreSQL доступен через **pg2** (`pg.core`). Слой БД использует параметризованные SQL-строки через `pg/execute`, а не HoneySQL.

Модули:

- `meteomax.db.users` — пользователи, геопозиция, избранные станции
- `meteomax.db.subs` — подписки на погодные уведомления

Схема таблиц и структуры API — см. [docs/data.md](data.md).

Миграции: `resources/migrations/`. При изменении схемы миграции добавляются вместе с кодом.

## HTTP интеграции

### Meteo API

`meteomax.meteo-data.core` — HTTP клиент (**http-kit**) с TTL-кэшем (**core.memoize**).

- `get-active-stations` — активные станции с фильтрацией по координатам, времени, поиску
- `get-station-info` — данные конкретной станции

### MAX Bot API

`meteomax.app.maxapi` инкапсулирует вызовы к `https://platform-api.max.ru`.

- `set-webhook`, `get-me`, `set-commands`
- `send-message`, `send-location`, `answer-callback`, `delete-message`
- `get-updates`, `get-subscriptions`, `upload-file`

## Webhook

`meteomax.app.webhook`:

- HTTP endpoint на **http-kit**, секрет генерируется через `SecureRandom`
- Валидирует заголовок `X-Max-Bot-Api-Secret`
- Маршрутизирует `update_type`: `message_created`, `message_callback`, `bot_started`

## Командный слой

`meteomax.app.command`:

- `/start`, `/help`, `/favs`, `/subs`
- `/sub_N` — редактирование подписки по id
- Текст 3+ символа → поиск станций (до 10 результатов)
- Геопозиция → 5 ближайших активных станций
- Callback-маршруты: `fav:toggle:`, `sub:new:`, `sub:time:`, `sub:day:`, `sub:ok:`, `sub:delete:`

## Планировщик

`meteomax.app.sender` — **chime**, раз в минуту:

1. Читает активные подписки через `meteomax.db.subs/get-all-active-subs`
2. Проверяет совпадение дня недели и времени
3. Получает погоду через `meteomax.meteo-data.core`
4. Отправляет сообщение через `meteomax.app.maxapi/send-message`

## Метрики

`meteomax.metrics.reg` — Prometheus-метрики через **iapetos**.
`meteomax.metrics.export` — endpoint `/metrics` через **http-kit**.
