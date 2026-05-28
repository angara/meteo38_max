(ns meteomax.app.command
  "Command handlers for MAX Weather Bot."
  (:require [clojure.string :as str]
            [meteomax.app.fmt :as fmt]
            [meteomax.app.maxapi :as maxapi]
            [meteomax.app.meteo-api :as meteo-api]
            [meteomax.app.subs :as subs]
            [meteomax.db.subscriptions :as subscriptions]
            [meteomax.db.users :as users]))


(defn- send-text
  [config chat-id text]
  (maxapi/send-message (:max-api-token config) chat-id text))


(defn handle-start
  "Handle /start command."
  [config db chat-id username first-name]
  (users/ensure-user! db chat-id username first-name)
  (send-text
   config
   chat-id
   (str "Привет, " (or first-name "друг") "! 👋\n\n"
        "Я бот погоды для Прибайкалья. Здесь можно получать данные о погоде "
        "со станций проекта https://meteo38.ru, настроить автоматическое "
        "уведомление в нужное время.\n\n"
        "Доступные команды:\n"
        "/help - справка\n"
        "/near - ближайшие станции\n"
        "/active - активные станции\n"
        "/favs - избранные станции\n"
        "/info <станция> - информация о станции\n"
        "/subs - мои подписки\n"
        "/sub <станция> <HH:MM> <дни> - подписка")))


(defn handle-help
  "Handle /help command."
  [config chat-id]
  (send-text
   config
   chat-id
   (str "📋 Справка по командам:\n\n"
        "/near [широта] [долгота] - ближайшие станции\n"
        "/active [поиск] - список активных станций\n"
        "/favs - избранные станции\n"
        "/info <станция> - подробная информация\n"
        "/map <станция> - показать на карте\n"
        "/subs - список подписок\n"
        "/sub <станция> <HH:MM> <дни> - создать подписку\n\n"
        "Вы можете отправить мне геопозицию, чтобы найти ближайшие станции.")))


(defn handle-near
  "Handle /near command with optional lat/lon or user location."
  [config db chat-id args]
  (let [[lat-str lon-str] args
        location (if (and lat-str lon-str)
                   {:latitude (Double/parseDouble lat-str)
                    :longitude (Double/parseDouble lon-str)}
                   (users/get-user-location db chat-id))]
    (if (nil? location)
      (send-text config chat-id "Не удалось определить местоположение. Отправьте координаты или геопозицию.")
      (let [{:keys [latitude longitude]} location
            result (meteo-api/get-active-stations config latitude longitude :last-hours 24)
            stations (:stations result)]
        (if (empty? stations)
          (send-text config chat-id "В окрестностях не найдено активных станций.")
          (send-text config
                     chat-id
                     (str "📍 Ближайшие станции:\n\n"
                          (str/join "\n"
                                    (map fmt/format-station-brief (take 10 stations))))))))))


(defn handle-active
  "Handle /active command with optional search."
  [config _db chat-id args]
  (let [search (when (seq args) (str/join " " args))
        result (meteo-api/get-active-stations config 52.28 104.28
                                              :last-hours 24
                                              :search search)
        stations (:stations result)]
    (if (empty? stations)
      (send-text config chat-id "Станции не найдены.")
      (send-text config
                 chat-id
                 (str "🌤 Активные станции:\n\n"
                      (str/join "\n"
                                (map fmt/format-station-brief (take 20 stations))))))))


(defn handle-favs
  "Handle /favs command."
  [config db chat-id]
  (let [favorites (users/get-favorites db chat-id)]
    (if (empty? favorites)
      (send-text config chat-id "У вас пока нет избранных станций. Добавьте станции командой /info.")
      (let [stations (map #(meteo-api/get-station-info config %) favorites)
            msg (str "⭐ Избранные станции:\n\n"
                     (str/join "\n"
                               (map fmt/format-station-brief stations)))]
        (send-text config chat-id msg)))))


(defn handle-info
  "Handle /info command with station name."
  [config db chat-id args]
  (if (empty? args)
    (send-text config chat-id "Укажите название станции. Пример: /info uiii")
    (let [station-name (first args)
          station (meteo-api/get-station-info config station-name)]
      (if (nil? station)
        (send-text config chat-id (str "Станция '" station-name "' не найдена."))
        (do
          (users/add-favorite! db chat-id station-name)
          (send-text config chat-id (fmt/format-station-info station)))))))


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


(defn handle-sub
  "Handle /sub command for subscription creation."
  [config db chat-id args]
  (let [input  (str/join " " args)
        parsed (subs/parse-subscription-input input)]
    (if parsed
      (let [sub (subscriptions/create-subscription! db
                                                    chat-id
                                                    (:station-name parsed)
                                                    (:time-str parsed)
                                                    (:days-of-week parsed))]
        (send-text config
                   chat-id
                   (str "Подписка создана:\n"
                        (:station_name sub) " в " (fmt/format-time (:time_str sub))
                        " (" (fmt/format-days-of-week (:days_of_week sub)) ")")))
      (send-text config
                 chat-id
                 (str "Укажите подписку в формате:\n"
                      "/sub <станция> <HH:MM> <дни>\n\n"
                      "Примеры:\n"
                      "/sub uiii 08:00 пн,вт,ср,чт,пт\n"
                      "/sub uiii 09:00 ежедневно")))))


(defn handle-location
  "Handle location message."
  [config db chat-id latitude longitude]
  (users/set-user-location! db chat-id latitude longitude)
  (handle-near config db chat-id nil))


(defn handle-text
  "Handle plain text message."
  [config chat-id _text]
  (send-text config chat-id "Неизвестная команда. Используйте /help для справки."))
