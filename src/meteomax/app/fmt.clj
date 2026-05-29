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


(defn station-keyboard [st]
  {:type    "inline_keyboard"
   :payload {:buttons [[{:type "callback" :text "⭐" :payload (str "fav:add:" st)}
                        {:type "link"     :text "🌐" :url (station-url st)}]]}})


(defn format-station-brief [{:keys [title descr elev distance last]}]
  (let [weather (when last (format-weather last))]
    (str/join "\n"
              (remove nil?
                      [(str "🔹 " title)
                       descr
                       (str
                        (when elev (str " ^" (int elev) " м"))
                        (when distance (str "  (" (int (/ distance 1000)) " км)")))
                       (when (seq weather) weather)]))))


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


(defn format-days-of-week [days-bitmask]
  (let [days [[1 "пн"] [2 "вт"] [4 "ср"] [8 "чт"] [16 "пт"] [32 "сб"] [64 "вс"]]]
    (->> days
         (filter (fn [[bit _]] (bit-test days-bitmask (int (/ (Math/log bit) (Math/log 2))))))
         (map second)
         (str/join ","))))
