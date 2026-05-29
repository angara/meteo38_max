# Структуры данных

## База данных

### users

| Колонка            | Тип                | Описание                                        |
|--------------------|--------------------|-------------------------------------------------|
| `chat_id`          | `TEXT PRIMARY KEY` | Идентификатор чата/пользователя в MAX Bot API   |
| `created_at`       | `TIMESTAMPTZ`      | Дата первого обращения                          |
| `updated_at`       | `TIMESTAMPTZ`      | Дата последнего обновления профиля              |
| `userinfo`         | `JSONB`            | Данные профиля от MAX API (имя, username и др.) |
| `favs`             | `JSONB`            | Избранные станции                               |
| `last_latlon`      | `JSONB`            | Последняя геопозиция в формате `[lat, lon]`     |
| `active`           | `BOOLEAN NOT NULL` | Пользователь активен (по умолчанию `true`)      |

### subs

| Колонка        | Тип                  | Описание                                                                                             |
|----------------|----------------------|------------------------------------------------------------------------------------------------------|
| `id`           | `SERIAL PRIMARY KEY` | Авто-инкремент                                                                                       |
| `created_at`   | `TIMESTAMPTZ`        | Дата создания подписки                                                                               |
| `chat_id`      | `TEXT NOT NULL`      | Чат, в который отправляется уведомление                                                              |
| `station_name` | `TEXT NOT NULL`      | Идентификатор метеостанции                                                                           |
| `time_str`     | `TEXT NOT NULL`      | Время уведомления в формате `HH:MM`                                                                  |
| `days_of_week` | `TEXT NOT NULL`      | Строка цифр 1–7 (пн=1, вт=2, ср=3, чт=4, пт=5, сб=6, вс=7). Например: `"135"` = пн, ср, пт; `"1234567"` = каждый день |
| `active`       | `BOOLEAN NOT NULL`   | Подписка активна (по умолчанию `true`)                                                               |

Миграции: `resources/migrations/`.

---

## Meteo API

Базовый URL: `METEO_API_URL` (по умолчанию `https://angara.net/meteo/api`).

### GET /active-stations

Параметры: `lat`, `lon`, `last-hours?`, `search?`

Ответ:

```json
{
  "stations": [
    {
      "st":       "uiii",
      "title":    "Иркутский аэропорт",
      "descr":    "г. Иркутск, ул. Ширямова, 101",
      "lat":      52.27,
      "lon":      104.35,
      "elev":     495.0,
      "distance": 2066.8,
      "last": { ... }
    }
  ]
}
```

### GET /station-info

Параметры: `st` (название станции)

Ответ:

```json
{
  "st":    "uiii",
  "title": "Иркутский аэропорт",
  "descr": "г. Иркутск, ул. Ширямова, 101",
  "lat":   52.27,
  "lon":   104.35,
  "elev":  495.0,
  "last":  { ... }
}
```

### Объект `last` — текущие метеоданные

| Поле      | Тип     | Единица | Описание                  |
|-----------|---------|---------|---------------------------|
| `t`       | float   | °C      | Температура воздуха       |
| `t_delta` | float   | °C      | Изменение за последний час |
| `d`       | float   | °C      | Температура точки росы    |
| `p`       | float   | гПа     | Атмосферное давление      |
| `h`       | float   | %       | Относительная влажность   |
| `w`       | float   | м/с     | Скорость ветра            |
| `g`       | float   | м/с     | Скорость порывов ветра    |
| `b`       | float   | °       | Направление ветра (0–360) |
| `r`       | float   | мм      | Осадки                    |

Все поля опциональны — станция может не передавать часть датчиков.

---

## MAX Bot API

Документация: [dev.max.ru](https://dev.max.ru/docs), [dev.tamtam.chat](https://dev.tamtam.chat/). Базовый URL: `https://platform-api.max.ru`.

### Входящий Update (webhook)

```json
{
  "update_type": "message_created",
  "message": {
    "sender":    { "name": "Иван", "username": "ivan" },
    "recipient": { "chat_id": "12345" },
    "body": {
      "text": "/start",
      "attachments": [ ... ]
    }
  }
}
```

Поддерживаемые `update_type`:

| Тип                | Поля                               |
|--------------------|------------------------------------|
| `message_created`  | `message`, `chat_id?`              |
| `message_callback` | `callback`                         |
| `bot_started`      | `chat_id`, `user`                  |

### Attachment: входящая геопозиция

```json
{
  "type":      "location",
  "latitude":  52.27,
  "longitude": 104.35
}
```

### Attachment: исходящий inline keyboard

```json
{
  "type": "inline_keyboard",
  "payload": {
    "buttons": [
      [
        { "type": "callback", "text": "⭐", "payload": "fav:add:uiii" },
        { "type": "link",     "text": "🌐", "url": "https://angara.net/meteo/st/uiii" }
      ]
    ]
  }
}
```

### Attachment: исходящая геопозиция

```json
{
  "type":    "location",
  "payload": { "lat": 52.27, "lon": 104.35 }
}
```

### POST /messages

Query param: `chat_id`. Body:

```json
{
  "text":        "текст сообщения",
  "format":      "markdown",
  "attachments": [ ... ],
  "link":        { ... }
}
```
