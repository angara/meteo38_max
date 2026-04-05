(ns meteomax.app.fmt
  "Formatting utilities for weather data display."
  (:require [clojure.string :as str]
            [java-time.api :as t]))

(def wind-directions
  "Wind direction arrows by degrees."
  {0 "↑" 45 "↗" 90 "→" 135 "↘"
   180 "↓" 225 "↙" 270 "←" 315 "↖"})

(defn deg-to-direction
  "Convert wind degrees to arrow direction."
  [deg]
  (if (nil? deg)
    "-"
    (let [sector (int (Math/round (/ (double deg) 45.0)))
          actual-deg (mod (* sector 45) 360)]
      (get wind-directions actual-deg "↑"))))

(defn format-temp
  "Format temperature with unit."
  [temp]
  (if (nil? temp)
    "-"
    (format "%d°C" (int (Math/round temp)))))

(defn format-pressure
  "Format pressure in hPa."
  [pressure]
  (if (nil? pressure)
    "-"
    (format "%d гПа" (int (Math/round pressure)))))

(defn format-wind
  "Format wind speed."
  [wind]
  (if (nil? wind)
    "-"
    (format "%.1f м/с" wind)))

(defn format-gust
  "Format gust speed."
  [gust]
  (if (nil? gust)
    "-"
    (format "%.1f м/с" gust)))

(defn format-humidity
  "Format humidity."
  [humidity]
  (if (nil? humidity)
    "-"
    (format "%d%%" (int (Math/round humidity)))))

(defn format-precip
  "Format precipitation."
  [precip]
  (if (nil? precip)
    "-"
    (format "%.1f мм" precip)))

(defn format-delta
  "Format change delta with sign."
  [delta]
  (cond
    (nil? delta) ""
    (pos? delta) (format " (+%d)" (int (Math/round delta)))
    (neg? delta) (format " (%d)" (int (Math/round delta)))
    :else " (0)"))

(defn format-station-info
  "Format station information for display.
   
   Parameters:
   - station: Station data map from API
   
   Returns: Formatted string"
  [{:keys [st title descr elev last_ts last distance]}]
  (let [t (:t last)
        p (:p last)
        w (:w last)
        g (:g last)
        b (:b last)
        d (:d last)
        h (:h last)
        r (:r last)
        t-delta (:t_delta last)
        p-delta (:p_delta last)
        w-delta (:w_delta last)]
    (str/join "\n"
      [(str "🌤 " title (when st (str " (" st ")")))
       (when descr (str "📍 " descr))
       (when distance (str "📏 Расстояние: " (format "%.1f" distance) " км"))
       (when elev (str "⛰ Высота: " (int elev) " м"))
       ""
       (str "🌡 Температура: " (format-temp t) (format-delta t-delta))
       (when d (str "💧 Точка росы: " (format-temp d)))
       (when h (str "🌊 Влажность: " (format-humidity h)))
       (str "🔽 Давление: " (format-pressure p) (format-delta p-delta))
       (str "💨 Ветер: " (format-wind w) (format-delta w-delta)
            (when g (str ", порывы " (format-gust g)))
            (when b (str " " (deg-to-direction b))))
       (when r (str "🌧 Осадки: " (format-precip r)))
       (when last-ts (str "⏰ " (t/format "dd.MM.yyyy HH:mm" (t/zoned-date-time last-ts))))])))

(defn format-station-brief
  "Format brief station info for lists."
  [{:keys [title st last distance]}]
  (let [t (:t last)
        w (:w last)
        b (:b last)]
    (str title
         (when st (str " (" st ")"))
         (when distance (str " — " (format "%.1f" distance) " км"))
         (when t (str ", " (format-temp t)))
         (when w (str ", " (format-wind w) (when b (str " " (deg-to-direction b))))))))

(defn format-time
  "Format time string."
  [time-str]
  (or time-str "--:--"))

(defn format-days-of-week
  "Format days of week bitmask to readable string.
   
   Days: 1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun
   127 = all days"
  [days]
  (let [day-names {1 "Пн" 2 "Вт" 4 "Ср" 8 "Чт" 16 "Пт" 32 "Сб" 64 "Вс"}
        active-days (filter #(bit-test days (int (Math/log % 2)))
                           (keys day-names))]
    (if (= days 127)
      "ежедневно"
      (if (empty? active-days)
        "-"
        (str/join ", " (map day-names (sort active-days)))))))

(comment
  ;; Test formatting:
  (format-temp -20.5)
  ;; => "-21°C"
  
  (deg-to-direction 90)
  ;; => "→"
  
  (format-days-of-week 127)
  ;; => "ежедневно"
  
  (format-days-of-week (+ 1 2 4 8 16))
  ;; => "Пн, Вт, Ср, Чт, Пт"
  
  (format-station-info
    {:st "uiii"
     :title "Иркутск"
     :descr "Аэропорт"
     :elev 495
     :distance 5.2
     :last_ts "2025-01-25T19:00:00+08:00"
     :last {:t -20.0 :t_delta 0.0
            :p 986.0 :p_delta 0.0
            :w 3.0 :w_delta 0.0
            :g 14.0
            :b 80.0
            :d -28.0
            :h 75}})
)
