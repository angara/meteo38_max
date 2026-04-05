(ns meteomax.app.command
  "Command handlers for MAX Weather Bot."
  (:require [meteomax.data.store :as store]
            [meteomax.app.fmt :as fmt]
            [mlib.max.botapi :as botapi]
            [taoensso.telemere :as log]))

(defn handle-start
  "Handle /start command."
  [config db chat-id username first-name]
  (store/ensure-user! db chat-id username first-name)
  (botapi/send-message
   (:max-api-token config)
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
        "/sub - редактировать подписку"))

(defn handle-help
  "Handle /help command."
  [config chat-id]
  (botapi/send-message
   (:max-api-token config)
   chat-id
   (str "📋 Справка по командам:\n\n"
        "/near [широта] [долгота] - ближайшие станции\n"
        "/active [поиск] - список активных станций\n"
        "/favs - избранные станции\n"
        "/info <станция> - подробная информация\n"
        "/map <станция> - показать на карте\n"
        "/subs - список подписок\n"
        "/sub - редактировать подписку\n\n"
        "Вы можете отправить мне геопозицию, чтобы найти ближайшие станции.")))

(defn handle-near
  "Handle /near command with optional lat/lon or user location."
  [config db chat-id args]
  (let [[lat-str lon-str] args
        location (if (and lat-str lon-str)
                   {:latitude (Double/parseDouble lat-str)
                    :longitude (Double/parseDouble lon-str)}
                   (store/get-user-location db chat-id))]
    (if (nil? location)
      (botapi/send-message
       (:max-api-token config)
       chat-id
       "Не удалось определить местоположение. Отправьте координаты или геопозицию.")
      (let [{:keys [latitude longitude]} location
            result (store/get-stations-near config latitude longitude :last-hours 24)
            stations (:stations result)]
        (if (empty? stations)
          (botapi/send-message
           (:max-api-token config)
           chat-id
           "В окрестностях не найдено активных станций.")
          (let [msg (str "📍 Ближайшие станции:\n\n"
                         (clojure.string/join "\n"
                           (map fmt/format-station-brief (take 10 stations))))]
            (botapi/send-message (:max-api-token config) chat-id msg)))))))

(defn handle-active
  "Handle /active command with optional search."
  [config db chat-id args]
  (let [search (when (seq args) (clojure.string/join " " args))
        result (store/get-stations-near config 52.28 104.28
                                        :last-hours 24
                                        :search search)
        stations (:stations result)]
    (if (empty? stations)
      (botapi/send-message
       (:max-api-token config)
       chat-id
       "Станции не найдены.")
      (let [msg (str "🌤 Активные станции:\n\n"
                     (clojure.string/join "\n"
                       (map fmt/format-station-brief (take 20 stations))))]
        (botapi/send-message (:max-api-token config) chat-id msg)))))

(defn handle-favs
  "Handle /favs command."
  [config db chat-id]
  (let [favorites (store/get-favorites db chat-id)]
    (if (empty? favorites)
      (botapi/send-message
       (:max-api-token config)
       chat-id
       "У вас пока нет избранных станций. Добавьте станции командой /info.")
      (let [stations (map #(store/get-station-info config %) favorites)
            msg (str "⭐ Избранные станции:\n\n"
                     (clojure.string/join "\n"
                       (map fmt/format-station-brief stations)))]
        (botapi/send-message (:max-api-token config) chat-id msg)))))

(defn handle-info
  "Handle /info command with station name."
  [config db chat-id args]
  (if (empty? args)
    (botapi/send-message
     (:max-api-token config)
     chat-id
     "Укажите название станции. Пример: /info uiii")
    (let [station-name (first args)
          station (store/get-station-info config station-name)]
      (if (nil? station)
        (botapi/send-message
         (:max-api-token config)
         chat-id
         (str "Станция '" station-name "' не найдена."))
        (do
          (store/add-favorite! db chat-id station-name)
          (botapi/send-message
           (:max-api-token config)
           chat-id
           (fmt/format-station-info station)))))))

(defn handle-subs
  "Handle /subs command."
  [config db chat-id]
  (let [subs (store/get-user-subs db chat-id)]
    (if (empty? subs)
      (botapi/send-message
       (:max-api-token config)
       chat-id
       (str "У вас нет активных подписок.\n\n"
            "Используйте /sub для создания подписки на уведомления о погоде."))
      (let [msg (str "📬 Ваши подписки:\n\n"
                     (clojure.string/join "\n\n"
                       (map #(str "• " (:station_name %)
                                  " в " (fmt/format-time (:time_str %))
                                  " (" (fmt/format-days-of-week (:days_of_week %)) ")")
                            subs)))]
        (botapi/send-message (:max-api-token config) chat-id msg)))))

(defn handle-sub
  "Handle /sub command for subscription editing."
  [config chat-id args]
  (botapi/send-message
   (:max-api-token config)
   chat-id
   "Функция редактирования подписок в разработке. Используйте inline-кнопки в полном режиме."))

(defn handle-location
  "Handle location message."
  [config db chat-id latitude longitude]
  (store/set-user-location! db chat-id latitude longitude)
  (handle-near config db chat-id nil))

(defn handle-text
  "Handle plain text message."
  [config chat-id text]
  (botapi/send-message
   (:max-api-token config)
   chat-id
   "Неизвестная команда. Используйте /help для справки."))

(comment
  ;; Test commands:
  (def config {:max-api-token "test"
               :meteo-api-url "https://angara.net/meteo/api"
               :meteo-api-auth "Bearer test"
               :meteo-api-timeout 5000})
  
  ;; These would require actual MAX API and DB connection
)
