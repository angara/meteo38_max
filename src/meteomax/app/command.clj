(ns meteomax.app.command
  "Command handlers for MAX Weather Bot."
  (:require [clojure.string :as str]
            [meteomax.app.fmt :as fmt]
            [meteomax.app.maxapi :as maxapi]
            [meteomax.meteo-data.core :as meteo-api]
            [meteomax.db.subs :as subscriptions]
            [meteomax.db.users :as users]
            [taoensso.telemere :refer [log!]]))


;;  TamTam/MAX error code when the user stops the bot
(def ^:private bot-stopped-code 909)


(defn- send!
  [config db chat-id text & opts]
  (let [result (apply maxapi/send-message (:max-api-token config) chat-id text opts)]
    (when (= bot-stopped-code (:error-code result))
      (log! {:level :info :id :command/user-blocked-bot
             :msg "User blocked the bot, deactivating" :data {:chat-id chat-id}})
      (users/set-active! db chat-id false))
    result))


(defn handle-start [config db chat-id user]
  (users/ensure-user! db chat-id user)
  (send! config db chat-id
         (str "Привет, " (or (:name user) "друг") "! 👋\n\n"
              "Я бот погоды для Прибайкалья. Здесь можно получать данные о погоде "
              "со станций проекта https://meteo38.ru, настроить автоматическое "
              "уведомление в нужное время.\n"
              "\n"
              "Используй /help для просмотра списка команд.\n")))


(defn handle-help [config db chat-id]
  (send! config db chat-id
         (str "📋 Справка по командам:\n\n"
              "/help - список команд\n"
              "/favs - избранные станции (или .))\n"
              "/subs - список подписок (или ,)\n"
              "\n"
              "Вы можете отправить геопозицию, чтобы найти ближайшие станции.\n"
              "\n"
              "Для поиска станции названию наберите текст не менее трех символов.\n")))


(defn handle-favs [config db chat-id]
  (let [favorites (users/get-favorites db chat-id)]
    (if (empty? favorites)
      (send! config db chat-id "У вас пока нет избранных станций.")
      (doseq [st-id favorites]
        (let [st (meteo-api/get-station-info config st-id)]
          (send! config db chat-id (fmt/format-station-brief st)
                 :format "html"
                 :attachments [(fmt/station-keyboard st-id true)]))))))


(defn handle-subs
  "Handle /subs command."
  [config db chat-id]
  (let [subs (subscriptions/get-user-subs db chat-id)]
    (if (empty? subs)
      (send! config db chat-id "У вас нет активных подписок.")
      (let [msg (str "📬 Ваши подписки:\n\n"
                     (str/join "\n\n"
                               (map (fn [sub]
                                      (let [st-info (meteo-api/get-station-info config (:station_name sub))
                                            title   (or (:title st-info) (:station_name sub))]
                                        (str "🔹 <u>" title "</u>"
                                             "\n" (:time_str sub)
                                             " - " (fmt/format-days-of-week (:days_of_week sub))
                                             "\n/sub_" (:id sub))))
                                    subs)))]
        (send! config db chat-id msg :format "html")))))


(defn- adjust-time [time-str delta]
  (let [[h m] (map #(Long/parseLong %) (str/split time-str #":"))
        total  (mod (+ (* h 60) m delta) 1440)]
    (format "%02d:%02d" (quot total 60) (mod total 60))))


(defn- sub-answer [config callback-id sub]
  (let [st-info (meteo-api/get-station-info config (:station_name sub))
        title   (or (:title st-info) (:station_name sub))]
    (maxapi/answer-callback (:max-api-token config) callback-id
                            :message {:text        (fmt/format-sub-message title (:time_str sub))
                                      :format      "html"
                                      :attachments [(fmt/subscription-keyboard (:id sub) (:days_of_week sub))]})))


(defn handle-sub-new [config db chat-id callback-id station-name]
  (if (>= (count (subscriptions/get-user-subs db chat-id)) 20)
    (do
      (maxapi/answer-callback (:max-api-token config) callback-id
                              :notification "Достигнут лимит подписок.")
      (send! config db chat-id "Достигнут лимит подписок.\n/subs"))
    (let [sub     (subscriptions/create-subscription! db chat-id station-name "08:00" "1234567")
          st-info (meteo-api/get-station-info config (:station_name sub))
          title   (or (:title st-info) (:station_name sub))]
      (maxapi/answer-callback (:max-api-token config) callback-id
                              :notification "Подписка создана")
      (send! config db chat-id
             (fmt/format-sub-message title (:time_str sub))
             :format "html"
             :attachments [(fmt/subscription-keyboard (:id sub) (:days_of_week sub))]))))


(defn handle-sub-time [config db chat-id callback-id sub-id delta]
  (if-let [sub (subscriptions/get-user-sub db sub-id chat-id)]
    (sub-answer config callback-id
                (subscriptions/update-sub! db sub-id
                                           (adjust-time (:time_str sub) delta)
                                           (:days_of_week sub)))
    (maxapi/answer-callback (:max-api-token config) callback-id
                            :notification "Подписка не найдена.")))


(defn handle-sub-day [config db chat-id callback-id sub-id day-num]
  (if-let [sub (subscriptions/get-user-sub db sub-id chat-id)]
    (let [days     (str (:days_of_week sub))
          day-str  (str day-num)
          new-days (if (str/includes? days day-str)
                     (str/replace days day-str "")
                     (str/join (sort (str days day-str))))]
      (sub-answer config callback-id
                  (subscriptions/update-sub! db sub-id (:time_str sub) new-days)))
    (maxapi/answer-callback (:max-api-token config) callback-id
                            :notification "Подписка не найдена.")))


(defn handle-sub-edit [config db chat-id sub-id]
  (if-let [sub (subscriptions/get-user-sub db sub-id chat-id)]
    (let [st-info (meteo-api/get-station-info config (:station_name sub))
          title   (or (:title st-info) (:station_name sub))]
      (send! config db chat-id
             (fmt/format-sub-message title (:time_str sub))
             :format "html"
             :attachments [(fmt/subscription-keyboard (:id sub) (:days_of_week sub))]))
    (send! config db chat-id "Подписка не найдена.")))


(defn handle-sub-ok [config db chat-id callback-id sub-id]
  (if-let [sub (subscriptions/get-user-sub db sub-id chat-id)]
    (let [st-info (meteo-api/get-station-info config (:station_name sub))
          title   (or (:title st-info) (:station_name sub))]
      (maxapi/answer-callback (:max-api-token config) callback-id
                              :message {:text        (fmt/format-sub-done title (:time_str sub) (:days_of_week sub) (:id sub))
                                        :format      "html"
                                        :attachments []}))
    (maxapi/answer-callback (:max-api-token config) callback-id
                            :notification "Подписка не найдена.")))


(defn handle-sub-delete [config db chat-id callback-id message-id sub-id]
  (if-let [sub (subscriptions/get-user-sub db sub-id chat-id)]
    (do
      (subscriptions/delete-sub! db (:id sub))
      (maxapi/answer-callback (:max-api-token config) callback-id
                              :notification "Подписка удалена")
      (maxapi/delete-message (:max-api-token config) message-id)
      (handle-subs config db chat-id))
    (maxapi/answer-callback (:max-api-token config) callback-id
                            :notification "Подписка не найдена.")))


(defn handle-fav-toggle [config db chat-id callback-id station-name]
  (let [favs        (users/get-favorites db chat-id)
        is-fav?     (some #{station-name} favs)
        new-favs    (if is-fav?
                      (vec (remove #{station-name} favs))
                      (conj (vec favs) station-name))
        new-is-fav? (not is-fav?)
        station-info (meteo-api/get-station-info config station-name)]
    (users/set-favs! db chat-id new-favs)
    (maxapi/answer-callback (:max-api-token config) callback-id
                            :notification (if is-fav? "Удалено из избранного" "Добавлено в избранное")
                            :message {:text        (fmt/format-station-brief station-info)
                                      :format      "html"
                                      :attachments [(fmt/station-keyboard station-name new-is-fav?)]})))


(defn handle-location [config db chat-id {:keys [latitude longitude]}]
  (users/set-user-location! db chat-id latitude longitude)
  (let [stations (take 5 (:stations (meteo-api/get-active-stations config :lat latitude :lon longitude :last-hours 24)))
        favs     (set (users/get-favorites db chat-id))]
    (if (empty? stations)
      (send! config db chat-id "В окрестностях не найдено активных станций.")
      (doseq [st stations]
        (send! config db chat-id (fmt/format-station-brief st)
               :format "html"
               :attachments [(fmt/station-keyboard (:st st) (contains? favs (:st st)))])))))


(defn- handle-search [config db chat-id text]
  (let [{:keys [latitude longitude]} (users/get-user-location db chat-id)
        lat      (or latitude (:default-lat config))
        lon      (or longitude (:default-lon config))
        stations (take 10 (:stations (meteo-api/get-active-stations config
                                                                    :lat lat
                                                                    :lon lon
                                                                    :last-hours 24
                                                                    :search text)))
        favs     (set (users/get-favorites db chat-id))]
    (if (empty? stations)
      (send! config db chat-id (str "Станции по запросу «" text "» не найдены."))
      (doseq [st stations]
        (send! config db chat-id (fmt/format-station-brief st)
               :format "html"
               :attachments [(fmt/station-keyboard (:st st) (contains? favs (:st st)))])))))


(defn handle-text [config db chat-id text]
  (if (>= (count text) 3)
    (handle-search config db chat-id text)
    (send! config db chat-id "Используйте /help для справки.")))

