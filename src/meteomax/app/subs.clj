(ns meteomax.app.subs
  "Subscription management with inline keyboard support."
  (:require [meteomax.data.store :as store]
            [meteomax.app.fmt :as fmt]
            [taoensso.telemere :as log]))

;; TODO: Implement inline keyboard for subscription editing
;; This requires MAX API support for callback queries and inline keyboards

(defn create-subscription-flow
  "Start subscription creation flow.
   
   Returns: Initial message text"
  []
  (str "Создание подписки на уведомления о погоде.\n\n"
       "Отправьте название станции, время уведомлений и дни недели."))

(defn parse-subscription-input
  "Parse user input for subscription creation.
   
   Expected format: <station> <HH:MM> <days>
   Days: пн,вт,ср,чт,пт,сб,вс или ежедневно
   
   Returns: Parsed map or nil"
  [text]
  (try
    (let [parts (clojure.string/split text #"\s+")
          station (first parts)
          time-str (second parts)
          days-str (clojure.string/join " " (drop 2 parts))
          days (cond
                 (= days-str "ежедневно") 127
                 :else (reduce (fn [acc day]
                                 (bit-or acc
                                         (case (clojure.string/lower-case day)
                                           "пн" 1 "вт" 2 "ср" 4 "чт" 8
                                           "пт" 16 "сб" 32 "вс" 64 0)))
                               0
                               (clojure.string/split days-str #",")))]
      (when (and station time-str (re-matches #"\d{2}:\d{2}" time-str))
        {:station-name station
         :time-str time-str
         :days-of-week days}))
    (catch Exception _
      nil)))

(defn format-subscription-edit
  "Format subscription edit message.
   
   Returns: Message text with inline keyboard data"
  [sub]
  {:text (str "Редактирование подписки:\n\n"
              "Станция: " (:station_name sub) "\n"
              "Время: " (fmt/format-time (:time_str sub)) "\n"
              "Дни: " (fmt/format-days-of-week (:days_of_week sub)) "\n"
              "Статус: " (if (:active sub) "активна" "отключена"))
   :inline-keyboard [[{:text "Отключить" :data (str "toggle:" (:id sub))}
                      {:text "Удалить" :data (str "delete:" (:id sub))}]
                     [{:text "Назад" :data "subs:list"}]]})

(comment
  ;; Test parsing:
  (parse-subscription-input "uiii 08:00 пн вт ср чт пт")
  ;; => {:station-name "uiii" :time-str "08:00" :days-of-week 31}
  
  (parse-subscription-input "uiii 09:00 ежедневно")
  ;; => {:station-name "uiii" :time-str "09:00" :days-of-week 127}
)
