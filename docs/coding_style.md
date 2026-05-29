# Правила оформления кода

## Стиль кода

1. **Один trailing newline** в конце файла

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

5. **Динамические vars** с `*` ear-muffs:
   ```clojure
   (def ^:dynamic *metric-hook* nil)
   ```

6. **Порядок импортов** — три группы, внутри каждой алфавитный порядок:
   1. `clojure.*` — стандартная библиотека
   2. Сторонние библиотеки
   3. Внутренние пространства имён (`meteomax.*`)

   `:import` указывается после `:require`, сначала `java.*`, затем сторонние классы.

   ```clojure
   (ns meteomax.app.example
     (:require [clojure.string :as str]
               [clojure.set :as set]
               [chime.core :as chime]
               [jsonista.core :as json]
               [taoensso.telemere :refer [log!]]
               [meteomax.app.fmt :as fmt]
               [meteomax.db.users :as users])
     (:import [java.time Instant]
              [com.example SomeClass]))
   ```

7. **Стандартные алиасы** для clojure.* библиотек — использовать общепринятые сокращения:
   ```clojure
   (:require [clojure.string :as str]
             [clojure.set :as set]
             [clojure.edn :as edn]
             [clojure.java.io :as io]
             [clojure.walk :as walk])
   ```
Для внешней JSON-библиотеки использовать алиас `:as json` (в проекте — `[jsonista.core :as json]`).
Для `taoensso.telemere` предпочитать `:refer [log!]`, а не вызовы через `log/log!`.

7. **Две пустые строки** между функциями верхнего уровня (`def`, `defn`, `comment`, `defstate`):
   ```clojure
   (defn- make-url [endpoint]
     (str api-base endpoint))


   (defn- request-opts [token]
     {:headers {"Authorization" token}
      :timeout 10000})

8. **Выравнивание let** — в блоках с 3+ биндингами выравнивать значения в одну колонку:
   ```clojure
   (let [chat       (:chat msg)
         text       (:text msg)
         message-id (:message_id msg)]
     ...)
   ```
   ```

9. **Проверка reflection** — в модулях, где используются вызовы нативных Java-методов (`.foo`, `new`, interop), проверять reflection warnings и при необходимости добавлять type hints.

10. **Предпочитать Clojure-эквиваленты Java interop** — если у вызова Java-метода есть прямой и понятный Clojure-эквивалент, использовать его. Например: `ex-message` вместо `.getMessage`, `clojure.string/starts-with?` вместо `.startsWith`.

11. **Предпочитать `some->` вместо `get-in`** — для навигации по вложенным ключам использовать `some->` с keyword-вызовами:
   ```clojure
   ;; плохо
   (get-in message [:body :attachments])

   ;; хорошо
   (some-> message :body :attachments)
   ```
   `get-in` допустим только когда ключи формируются динамически или путь передаётся как аргумент.

12. **Поддерживать документацию в актуальном состоянии** — если изменение кода меняет структуру проекта, конфигурацию, команды, поведение или архитектурные допущения, в том же изменении обновлять `README.md` и соответствующие файлы в `docs/`.

## Логирование

Использовать **taoensso/telemere** через `log!`:

```clojure
(require '[taoensso.telemere :refer [log!]])

(log! {:level :warn
       :id :api/error
       :msg "API error"
       :data {:url url :status status}})
```

Уровни: `:debug`, `:info`, `:warn`, `:error`

Правила:
- **Всегда задавать `:id`** — namespaced keyword, идентифицирующий событие (например `:api/error`, `:bot/message-sent`).
- Структурированные данные передавать в `:data`.
- Текст сообщения — в `:msg`.

## Обработка ошибок

Структурированное логирование с контекстом:

```clojure
(try
  (api-call)
  (catch Exception e
    (log! {:level :error
           :id :api/exception
           :msg "API call failed"
           :data {:error (ex-message e)}})))
```

## Пользовательский интерфейс

- Все пользовательские тексты на **русском языке**
- Форматирование погоды с единицами измерения (°C, гПа, м/с)
- Направление ветра в виде стрелок (↑, ↗, →, ↘, ↓, ↙, ←, ↖)

## Тестирование

```clojure
(ns meteomax.app.command-test
  (:require [clojure.test :refer :all]
            [meteomax.app.command :as command]))

(deftest test-format-temperature
  (is (= "-20°C" (command/format-temp -20.0))))
```
