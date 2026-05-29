# Инструменты и окружение

## Переменные окружения

Локальные переменные загружаются из `.env` (не коммитится). Пример — `.env.example`.

```bash
cp .env.example .env
```

| Переменная          | Обязательна | По умолчанию                       | Описание                                          |
|---------------------|-------------|------------------------------------|---------------------------------------------------|
| `MAX_API_TOKEN`     | Да          | —                                  | Токен бота MAX                                    |
| `DATABASE_URL`      | Да          | —                                  | PostgreSQL connection URI                         |
| `METEO_API_AUTH`    | Да          | —                                  | Заголовок авторизации для meteo API               |
| `WEBHOOK_URL`       | Да          | —                                  | Публичный URL webhook для регистрации в MAX API   |
| `METEO_API_URL`     | Нет         | `https://angara.net/meteo/api`     | URL API meteo_data                                |
| `METEO_API_TIMEOUT` | Нет         | `5000`                             | Таймаут HTTP запросов (мс)                        |
| `WEBHOOK_PATH`      | Нет         | путь из `WEBHOOK_URL`              | Переопределить path webhook endpoint              |
| `WEBHOOK_BIND`      | Нет         | `localhost`                        | Адрес локального webhook сервера                  |
| `WEBHOOK_PORT`      | Нет         | `8005`                             | Порт локального webhook сервера                   |
| `METRICS_BIND`      | Нет         | `localhost`                        | Адрес сервера метрик                              |
| `METRICS_PORT`      | Нет         | `7937`                             | Порт сервера метрик                               |
| `TIMEZONE`          | Нет         | `Asia/Irkutsk`                     | Часовой пояс приложения                           |

## Dev-окружение

```bash
make dev-env        # запустить PostgreSQL в Docker
make dev            # запустить nREPL
make dev-env-stop   # остановить PostgreSQL
```

PostgreSQL (`docker-compose.dev.yml`): порт `5437`, база/пользователь/пароль — `meteomax`.

## Make-команды

| Команда             | Описание                              |
|---------------------|---------------------------------------|
| `make dev-env`      | Запустить PostgreSQL в Docker         |
| `make dev`          | Запустить nREPL с dev конфигом        |
| `make dev-env-stop` | Остановить PostgreSQL                 |
| `make install`      | Загрузить зависимости                 |
| `make build`        | Собрать uberjar                       |
| `make run`          | Запустить uberjar                     |
| `make lint`         | Запустить clj-kondo                   |
| `make check-reflect`| Проверить reflection warnings         |
| `make test`         | Запустить тесты                       |

## Сборка

```bash
make build
# результат: target/meteomax-1.0.X-standalone.jar

make run
```

## Тесты

```bash
make test
# или
clojure -M:test -e "(require 'clojure.test ...) (clojure.test/run-all-tests #\"meteomax.*\")"
```

## Линтинг и проверки

```bash
make lint           # clj-kondo по src/
make check-reflect  # reflection warnings для Java interop
```

`check-reflect` нужен при добавлении или изменении Java interop (`.method`, `new`, type hints).
