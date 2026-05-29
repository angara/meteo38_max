(ns meteomax.app.fmt
  (:require [clojure.string :as str]))


(def ^:private wind-arrows ["↑" "↗" "→" "↘" "↓" "↙" "←" "↖"])


(defn- wind-arrow [bearing]
  (when (and bearing (<= 0 bearing) (< bearing 360))
    (wind-arrows (int (mod (Math/floor (/ (+ (double bearing) 22) 45)) 8)))))


(defn- hpa->mmhg [hpa]
  (when hpa (int (/ hpa 1.3332239))))


(defn- float1 [x]
  (when x
    (let [rounded (Math/round (* (double x) 10.0))]
      (if (zero? (mod rounded 10))
        (str (quot rounded 10))
        (str (/ (double rounded) 10.0))))))


(defn- float1plus [x]
  (when-let [s (float1 x)]
    (if (pos? (double x)) (str "+" s) s)))


(defn- format-weather [{:keys [t t_delta p w g b]}]
  (let [temp-str (when t
                   (str (float1plus t) "°C"
                        (when t_delta
                          (cond (> t_delta 0.8) " ↑"
                                (< t_delta -0.8) " ↓"))))
        pres-str (when p (str (hpa->mmhg p) " мм.рт.ст."))
        wind-str (when w
                   (str (int w)
                        (when (and g (not= (int w) (int g))) (str "-" (int g)))
                        " м/с"
                        (when-let [a (wind-arrow b)] (str " " a))))]
    (->> [temp-str pres-str wind-str] (remove nil?) (str/join "  "))))


(defn station-url [st]
  (str "https://angara.net/meteo/st/" st))


(defn station-keyboard [st is-fav?]
  {:type    "inline_keyboard"
   :payload {:buttons [[{:type "callback" :text (if is-fav? "⭐" "☆") :payload (str "fav:toggle:" st)}
                        {:type "callback" :text "⏰" :payload (str "sub:new:" st)}
                        {:type "link"     :text "🌐" :url (station-url st)}]]}})


(defn format-station-brief [{:keys [title descr elev distance last]}]
  (let [weather (when last (format-weather last))]
    (str/join "\n"
              (remove nil?
                      [(str "🔹 <u>" title "</u>")
                       descr
                       (when (or elev distance)
                         (str "<i>"
                              (when elev (str "^" (int elev) " м"))
                              (when distance (str "  (" (int (/ distance 1000)) " км)"))
                              "</i>"))
                       (when (seq weather)
                         (str "<b>" weather "</b>"))]))))


(defn format-station-info [{:keys [st title descr elev last]}]
  (let [weather (when last (format-weather last))]
    (str/join "\n"
              (remove nil?
                      [(str "🔹 " title)
                       descr
                       (when (seq weather) weather)
                       (str "/info " st (when elev (str "  ^" (int elev) " м")))]))))


(defn format-time [time-str]
  time-str)


(def ^:private day-defs
  [[1 "пн"] [2 "вт"] [3 "ср"] [4 "чт"] [5 "пт"] [6 "сб"] [7 "вс"]])


(defn format-days-of-week [days]
  (let [s (str days)]
    (->> day-defs
         (filter (fn [[d _]] (str/includes? s (str d))))
         (map second)
         (str/join ", "))))


(defn format-sub-message [title time-str]
  (str "🔹 <u>" title "</u>\n⏰ Время рассылки - " time-str))


(defn format-sub-done [title time-str days sub-id]
  (str (format-sub-message title time-str) 
       "\n📅 " (format-days-of-week days) 
       "\n/sub_" sub-id))


(defn subscription-keyboard [sub-id days]
  (let [s       (str days)
        day-btn (fn [[d label]]
                  {:type    "callback"
                   :text    (if (str/includes? s (str d)) label "--")
                   :payload (str "sub:day:" d ":" sub-id)})]
    {:type    "inline_keyboard"
     :payload {:buttons [[{:type "callback" :text "-1 ч"  :payload (str "sub:time:-60:" sub-id)}
                          {:type "callback" :text "-10 м" :payload (str "sub:time:-10:" sub-id)}
                          {:type "callback" :text "+10 м" :payload (str "sub:time:+10:" sub-id)}
                          {:type "callback" :text "+1 ч"  :payload (str "sub:time:+60:" sub-id)}]
                         (mapv day-btn day-defs)
                         [{:type "callback" :text "✅ Ок"      :payload (str "sub:ok:" sub-id)}
                          {:type "callback" :text "❌ Удалить" :payload (str "sub:delete:" sub-id)}]]}}))
