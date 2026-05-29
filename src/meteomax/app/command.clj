(ns meteomax.app.command
  "Command handlers for MAX Weather Bot."
  (:require [clojure.string :as str]
            [meteomax.app.fmt :as fmt]
            [meteomax.app.maxapi :as maxapi]
            [meteomax.meteo-data.core :as meteo-api]
            [meteomax.db.subs :as subscriptions]
            [meteomax.db.users :as users]))


(defn- send-text
  [config chat-id text]
  (maxapi/send-message (:max-api-token config) chat-id text))


(defn handle-start [config db chat-id user]
  (users/ensure-user! db chat-id user)
  (send-text config chat-id
               (str "Привет, " (or (:name user) "друг") "! 👋\n\n"
                    "Я бот погоды для Прибайкалья. Здесь можно получать данные о погоде "
                    "со станций проекта https://meteo38.ru, настроить автоматическое "
                    "уведомление в нужное время.\n"
                    "\n"
                    "Используй /help для просмотра списка команд.\n")))


(defn handle-help [config chat-id]
  (send-text config chat-id
             (str "📋 Справка по командам:\n\n"
                  "/help - список команд\n"
                  "/favs - избранные станции или просто . (точка)\n"
                  "/subs - список подписок или просто , (запятая)\n"
                  "Вы можете отправить мне геопозицию, чтобы найти ближайшие станции.\n"
                  "Для поиска станции наберите текст не менее трех символов.\n")))


(defn handle-favs [config db chat-id]
  (let [favorites (users/get-favorites db chat-id)]
    (if (empty? favorites)
      (send-text config chat-id "У вас пока нет избранных станций. Добавьте станции командой /info.")
      (let [stations (map #(meteo-api/get-station-info config %) favorites)
            msg (str "⭐ Избранные станции:\n\n"
                     (str/join "\n"
                               (map fmt/format-station-brief stations)))]
        (send-text config chat-id msg)))))


(defn handle-subs
  "Handle /subs command."
  [config db chat-id]
  (let [subs (subscriptions/get-user-subs db chat-id)]
    (if (empty? subs)
      (send-text config
                 chat-id
                 (str "У вас нет активных подписок.\n\n"
                      "Используйте /sub для создания подписки на уведомления о погоде."))
      (let [msg (str "📬 Ваши подписки:\n\n"
                     (str/join "\n\n"
                               (map #(str "• " (:station_name %)
                                          " в " (fmt/format-time (:time_str %))
                                          " (" (fmt/format-days-of-week (:days_of_week %)) ")"
                                          (when-not (:active %) " [выключена]"))
                                    subs)))]
        (send-text config chat-id msg)))))


(defn handle-location [config db chat-id {:keys [latitude longitude]}]
  (users/set-user-location! db chat-id latitude longitude)
  (let [stations (take 5 (:stations (meteo-api/get-active-stations config :lat latitude :lon longitude :last-hours 24)))]
    (if (empty? stations)
      (send-text config chat-id "В окрестностях не найдено активных станций.")
      (doseq [st stations]
        (maxapi/send-message (:max-api-token config) chat-id
                             (fmt/format-station-brief st)
                             :format "html"
                             :attachments [(fmt/station-keyboard (:st st))])))))


(defn- handle-search [config db chat-id text]
  (let [{:keys [latitude longitude]} (users/get-user-location db chat-id)
        lat (or latitude (:default-lat config))
        lon (or longitude (:default-lon config))
        stations (take 10 (:stations (meteo-api/get-active-stations config
                                                                    :lat lat
                                                                    :lon lon
                                                                    :last-hours 24
                                                                    :search text)))]
    (if (empty? stations)
      (send-text config chat-id (str "Станции по запросу «" text "» не найдены."))
      (doseq [st stations]
        (maxapi/send-message (:max-api-token config) chat-id
                             (fmt/format-station-brief st)
                             :format "html"
                             :attachments [(fmt/station-keyboard (:st st))])))))


(defn handle-text [config db chat-id text]
  (if (>= (count text) 3)
    (handle-search config db chat-id text)
    (send-text config chat-id "Используйте /help для справки.")))

