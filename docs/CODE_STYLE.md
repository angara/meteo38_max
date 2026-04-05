# Правила оформления кода (на основе meteo38_bot)

## Общие принципы

1. **Язык**: Clojure 1.12+
2. **Стиль**: Идиоматичный Clojure с trailing запятыми в коллекциях
3. **Документация**: Обширные `comment` блоки с примерами REPL


## Именование неймспейсов

- `maxbot.*` - код приложения
- `maxbot.app.*` - бизнес-логика (команды, диспетчеризация, форматирование)
- `maxbot.data.*` - слой данных (API, БД, кэш)
- `maxbot.metrics.*` - метрики
- `mlib.*` - общие утилиты
- `mlib.max.*` - утилиты для MAX API


## Управление жизненным циклом

Использовать библиотеку **Mount**:

```clojure
(require '[mount.core :as mount])

(mount/defstate config
  :start (make-config)
  :stop (cleanup-config))
```

Основные состояния:
- `config` - конфигурация
- `dbc` - подключение к БД
- `poller` - сервис polling'а
- `sender-proc` - планировщик рассылок
- `metrics-endpoint` - HTTP сервер метрик


## Логирование

Использовать **taoensso/telemere**:

```clojure
(require '[taoensso.telemere :as log])

(log/log! :warn {:msg "API error" :url url :status status})
```

Уровни: `:trace`, `:debug`, `:info`, `:warn`, `:error`


## Валидация конфигурации

Использовать **Malli** схемы:

```clojure
(require '[malli.core :as m]
         '[malli.error :as me])

(def ConfigSchema
  [:map
   [:max-api-token string?]
   [:database-url string?]
   [:meteo-api-url {:default "https://angara.net/meteo/api"} string?]
   [:meteo-api-timeout {:default 5000} pos-int?]])

(defn validate-config [cfg]
  (if (m/validate ConfigSchema cfg)
    cfg
    (throw (ex-info "Invalid config" (me/humanize (m/explain ConfigSchema cfg))))))
```

## Работа с БД

Использовать **HugSQL** - SQL в `.sql` файлах:

```sql
-- :name get-user :? :1
-- :doc Получить пользователя по chat_id
SELECT * FROM users WHERE chat_id = :chat_id
```

```clojure
(require '[hugsql.core :as hug])
(hug/def-db-fns "maxbot/data/api.sql")
```

Connection pool через **pg2**:

```clojure
(require '[pg.core :as pg])

(defn make-pool [url]
  (pg/make-connection-pool {:jdbc-url url}))
```

## Кэширование

**core.memoize** для TTL кэширования API ответов:

```clojure
(require '[clojure.core.memoize :as memo])

(def stations-cache (memo/ttl fetch-stations :ttl/threshold 120000)) ; 2 мин
```

**core.cache** для пользовательских данных:

```clojure
(require '[clojure.core.cache :as cache])

(def user-locations (atom (cache/ttl-cache-factory {} :ttl 7200000))) ; 2 часа
```

## HTTP клиент

Использовать **http-kit**:

```clojure
(require '[org.httpkit.client :as http])

(defn fetch [url headers]
  (let [{:keys [status body]} @(http/get url {:headers headers})]
    (when (= 200 status)
      (jsonista/read-value body))))
```

## Обработка ошибок

Структурированное логирование с контекстом:

```clojure
(try
  (api-call)
  (catch Exception e
    (log/log! :error {:msg "API call failed" :error (.getMessage e)})))
```

## Метрики

Использовать **iapetos**:

```clojure
(require '[iapetos.core :as prom])

(def counter
  (prom/counter
    :messages_processed_total
    {:help "Total number of processed messages"}))

(prom/inc counter)
```

## Стиль кода

1. **Trailing запятые** в коллекциях:
   ```clojure
   {:a 1
    :b 2,}
   ```

2. **Comment блоки** с примерами REPL в конце файлов:
   ```clojure
   (comment
     ;; Пример использования
     (fetch-stations 52.0 104.0)
     ;; => {:stations [...]}
   )
   ```

3. **Предикаты** с `?` суффиксом:
   ```clojure
   (defn valid-location? [lat lon]
     (and (<= -90 lat 90) (<= -180 lon 180)))
   ```

4. **Приватные функции** с `^:private` или `-` префиксом

5. **Динамические vars** с `*` для хуков:
   ```clojure
   (def ^:dynamic *metric-hook* nil)
   ```

## Пользовательский интерфейс

- Все пользовательские тексты на **русском языке**
- Форматирование погоды с единицами измерения (°C, гПа, м/с)
- Направление ветра в виде стрелок (↑, ↗, →, ↘, ↓, ↙, ←, ↖)

## Тестирование

```clojure
(ns maxbot.app.command-test
  (:require [clojure.test :refer :all]
            [maxbot.app.command :as command]))

(deftest test-format-temperature
  (is (= "-20°C" (command/format-temp -20.0))))
```

## Сборка

Использовать **tools.build**:

```bash
clojure -T:build uber
```

Результат: `target/maxbot-1.0.X-standalone.jar`

## Линтинг

Использовать **clj-kondo**:

```bash
clj-kondo --lint src
```

Конфигурация в `.clj-kondo/config.edn`:
```edn
{:lint-as {pg.core/with-transaction clojure.core/let}}
```
