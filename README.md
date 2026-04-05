# MAX Weather Bot

Чат-бот для мессенджера MAX, предоставляющий информацию о погоде в Прибайкалье в реальном времени с настраиваемой рассылкой.

## Описание

Бот получает данные о погоде со станций проекта [meteo38.ru](https://github.com/angara/meteo_data) и позволяет пользователям:

- Просматривать текущую погоду на ближайших станциях
- Получать информацию о конкретных метеостанциях
- Настраивать автоматические уведомления о погоде в заданное время
- Искать станции по названию или местоположению
- Добавлять станции в избранное

## Архитектура

Проект построен на Clojure и использует следующие компоненты:

- **Mount** - управление жизненным циклом компонентов
- **http-kit** - HTTP клиент для API meteo_data и сервер для метрик
- **Malli** - валидация конфигурации
- **HugSQL** - работа с PostgreSQL
- **Chime** - планировщик для рассылок
- **iapetos** - Prometheus метрики
- **telemere** - логирование

### Структура проекта

```
src/
├── maxbot/                    # Основное приложение
│   ├── main.clj              # Точка входа
│   ├── config.clj            # Конфигурация
│   ├── app/
│   │   ├── command.clj       # Обработчики команд
│   │   ├── dispatch.clj      # Маршрутизация обновлений
│   │   ├── fmt.clj           # Форматирование данных
│   │   ├── inbound.clj       # Диспетчер входящих сообщений
│   │   ├── sender.clj        # Планировщик рассылок
│   │   ├── serv.clj          # Сервис polling'а MAX
│   │   └── subs.clj          # Управление подписками
│   ├── data/
│   │   ├── api.sql           # SQL запросы (HugSQL)
│   │   ├── init.sql          # Схема БД
│   │   ├── meteo_api.clj     # Клиент meteo_data API
│   │   ├── pg.clj            # PostgreSQL connection pool
│   │   ├── sql.clj           # HugSQL функции
│   │   └── store.clj         # Кэширующий слой данных
│   └── metrics/
│       ├── export.clj        # HTTP endpoint для метрик
│       └── reg.clj           # Определение метрик
└── mlib/                      # Общие утилиты
    ├── envvar.clj            # Утилиты для env переменных
    └── max/
        └── botapi.clj        # Клиент MAX Bot API
```

## API meteo_data

Бот использует API из [github.com/angara/meteo_data](https://github.com/angara/meteo_data):

### Доступные эндпоинты

- **GET /meteo/api/active-stations** - список активных станций
  - Параметры: `search?`, `lat?`, `lon?`, `last-hours?`
  
- **GET /meteo/api/station-info** - информация о станции
  - Параметры: `st` (название станции)
  
- **GET /meteo/api/station-hourly** - почасовые данные
  - Параметры: `st`, `ts-beg?`, `ts-end?`

### Метеопараметры

| Код | Описание | Единица |
|-----|----------|---------|
| `t` | Температура воздуха | °C |
| `d` | Температура точки росы | °C |
| `p` | Атмосферное давление | гПа |
| `h` | Относительная влажность | % |
| `w` | Скорость ветра | м/с |
| `g` | Скорость порывов ветра | м/с |
| `b` | Направление ветра | градусы (0-360) |
| `r` | Осадки | мм |

## Настройка и установка

### Требования

- Java 21+
- Clojure CLI tools
- PostgreSQL 14+
- MAX Bot API токен (получить через [MAX для разработчиков](https://dev.max.ru/docs))

### Конфигурация

Необходимые переменные окружения:

| Переменная | Обязательна | По умолчанию | Описание |
|------------|-------------|--------------|----------|
| `MAX_API_TOKEN` | Да | - | Токен бота MAX |
| `DATABASE_URL` | Да | - | PostgreSQL connection URI |
| `METEO_API_URL` | Нет | `https://angara.net/meteo/api` | URL API meteo_data |
| `METEO_API_AUTH` | Да | - | Заголовок авторизации для meteo API |
| `METEO_API_TIMEOUT` | Нет | `5000` | Таймаут HTTP запросов (мс) |
| `METRICS_BIND` | Нет | `localhost` | Адрес сервера метрик |
| `METRICS_PORT` | Нет | `7937` | Порт сервера метрик |
| `TIMEZONE` | Нет | `Asia/Irkutsk` | Часовой пояс приложения |

### Запуск в разработке

```bash
# Установка зависимостей
make install

# Запуск REPL
make dev

# Линтинг
make lint
```

### Сборка и запуск

```bash
# Сборка uberjar
make build

# Запуск
make run
```

### Docker

```bash
# Сборка образа
make docker-build

# Запуск контейнера
make docker-run
```

## Команды бота

| Команда | Описание |
|---------|----------|
| `/start` | Приветственное сообщение |
| `/help` | Справка по командам |
| `/near` | Ближайшие станции (по геолокации) |
| `/active` | Список активных станций |
| `/favs` | Избранные станции |
| `/info <станция>` | Информация о станции |
| `/map <станция>` | Станция на карте |
| `/subs` | Список подписок |
| `/sub` | Редактировать подписку |

## Метрики

Бот экспортирует Prometheus метрики на `http://localhost:7937/metrics`:

- Количество обработанных сообщений
- Время обработки запросов
- Количество ошибок API
- Активные подписки

## Развертывание

### Systemd

Используйте файл `maxbot.service`:

```ini
[Unit]
Description=MAX Weather Bot
After=network.target

[Service]
Type=simple
User=maxbot
WorkingDirectory=/opt/maxbot
ExecStart=/usr/bin/java -jar target/maxbot-standalone.jar
EnvironmentFile=/etc/maxbot/.env
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

## Лицензия

MIT

## Ссылки

- [MAX для разработчиков](https://dev.max.ru/docs)
- [meteo_data API](https://github.com/angara/meteo_data)
- [meteo38_bot (референс)](https://github.com/angara/meteo38_bot)
