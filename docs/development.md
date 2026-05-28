# Инструменты и окружение

## Переменные окружения

Локальные переменные окружения загружаются из файла `.env` (не коммитится в git).

Аннотированные примеры всех переменных находятся в `.env.example`.

```bash
cp .env.example .env
```

Основные переменные:
- `MAX_API_TOKEN` - токен MAX Bot API
- `DATABASE_URL` - PostgreSQL connection URI
- `METEO_API_URL` - базовый URL Meteo API
- `METEO_API_AUTH` - авторизация для Meteo API
- `METEO_API_TIMEOUT` - таймаут HTTP запросов (мс)
- `WEBHOOK_URL` - публичный URL webhook для регистрации в MAX API
- `WEBHOOK_PATH` - path webhook endpoint, если нужно переопределить путь из `WEBHOOK_URL`
- `WEBHOOK_BIND` - адрес локального webhook сервера
- `WEBHOOK_PORT` - порт локального webhook сервера
- `METRICS_PORT` - порт metrics сервера
- `METRICS_BIND` - адрес metrics сервера
- `TIMEZONE` - таймзона приложения

## Dev-окружение

```bash
make dev-env        # запустить PostgreSQL в Docker
make dev            # запустить nREPL (в отдельном терминале)
make dev-env-stop   # остановить PostgreSQL
```

Конфигурация PostgreSQL для разработки — `docker-compose.dev.yml`:
- Порт: `5432`
- База: `meteomax`
- Пользователь/пароль: `meteomax` / `meteomax`

Make-команды:

| Команда | Описание |
|----------|-----------|
| `make dev-env` | Запустить PostgreSQL в Docker |
| `make dev` | Запустить nREPL с dev конфигом |
| `make dev-env-stop` | Остановить PostgreSQL |
| `make install` | Загрузить все зависимости |
| `make build` | Собрать uberjar |
| `make check-reflect` | Проверить reflection warnings |

## Сборка

Использовать **tools.build**:

```bash
clojure -T:build uber
```

Результат: `target/meteomax-1.0.X-standalone.jar`

## Линтинг

Использовать **clj-kondo**:

```bash
make lint
# или
clj-kondo --lint src
```

Конфигурация в `.clj-kondo/config.edn`:
```edn
{:lint-as {pg.core/with-transaction clojure.core/let}}
```

## Reflection

Для модулей с Java interop проверяйте reflection warnings:

```bash
make check-reflect
```
