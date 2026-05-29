(ns meteomax.app.fmt-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [meteomax.app.fmt :as fmt]))

(def ^:private wind-arrow  #'fmt/wind-arrow)
(def ^:private hpa->mmhg   #'fmt/hpa->mmhg)
(def ^:private float1      #'fmt/float1)
(def ^:private float1plus  #'fmt/float1plus)


;; --- wind-arrow ---

(deftest wind-arrow-cardinal-directions
  (is (= "↑" (wind-arrow 0)))
  (is (= "↗" (wind-arrow 45)))
  (is (= "→" (wind-arrow 90)))
  (is (= "↘" (wind-arrow 135)))
  (is (= "↓" (wind-arrow 180)))
  (is (= "↙" (wind-arrow 225)))
  (is (= "←" (wind-arrow 270)))
  (is (= "↖" (wind-arrow 315))))

(deftest wind-arrow-boundary-cases
  (testing "22° still rounds to North"
    (is (= "↑" (wind-arrow 22))))
  (testing "23° rounds to NE"
    (is (= "↗" (wind-arrow 23))))
  (testing "359° rounds to North"
    (is (= "↑" (wind-arrow 359)))))

(deftest wind-arrow-invalid
  (is (nil? (wind-arrow nil)))
  (is (nil? (wind-arrow 360)))
  (is (nil? (wind-arrow -1))))


;; --- hpa->mmhg ---

(deftest hpa->mmhg-conversion
  (is (= 740 (hpa->mmhg 986.7)))
  (is (= 750 (hpa->mmhg 1000.0)))
  (is (nil? (hpa->mmhg nil))))


;; --- float1 / float1plus ---

(deftest float1-formatting
  (is (= "1"     (float1 1.0)))
  (is (= "1.1"   (float1 1.1)))
  (is (= "-10"   (float1 -10.0)))
  (is (= "-10.2" (float1 -10.23)))
  (is (= "0"     (float1 0.0)))
  (is (nil?      (float1 nil))))

(deftest float1plus-sign
  (is (= "+1"   (float1plus 1.0)))
  (is (= "+1.5" (float1plus 1.5)))
  (is (= "-5"   (float1plus -5.0)))
  (is (= "0"    (float1plus 0.0)))
  (is (nil?     (float1plus nil))))


;; --- format-station-brief ---

(deftest format-station-brief-with-weather
  (let [result (fmt/format-station-brief
                {:st "uiii" :title "Иркутский аэропорт"
                 :descr "г. Иркутск, ул. Ширямова, 101"
                 :elev 495.0 :distance 2066.8
                 :last {:t -20.0 :t_delta 0.0 :p 986.0 :w 3.0 :g 14.0 :b 80.0}})
        lines (str/split-lines result)]
    (is (str/includes? result "🔹 Иркутский аэропорт"))
    (is (str/includes? result "г. Иркутск, ул. Ширямова, 101"))
    (is (str/includes? result "^495 м"))
    (is (str/includes? result "(2 км)"))
    (is (str/includes? (last lines) "-20°C"))
    (is (str/includes? (last lines) "мм.рт.ст."))
    (is (str/includes? (last lines) "м/с"))))

(deftest format-station-brief-no-weather
  (let [result (fmt/format-station-brief {:st "abc" :title "Тест"
                                          :descr "Описание"})]
    (is (str/includes? result "🔹 Тест"))
    (is (str/includes? result "Описание"))))

(deftest format-station-brief-no-optional-fields
  (let [result (fmt/format-station-brief {:st "abc" :title "Тест"})]
    (is (str/includes? result "🔹 Тест"))
    (is (not (str/includes? result "null")))))


;; --- format-station-info ---

(deftest format-station-info-with-weather
  (let [result (fmt/format-station-info
                {:st "uiii" :title "Иркутский аэропорт"
                 :descr "г. Иркутск"
                 :elev 495.0
                 :last {:t -20.0 :t_delta 0.0 :p 986.0 :w 3.0 :g 14.0 :b 80.0}})]
    (is (str/includes? result "🔹 Иркутский аэропорт"))
    (is (str/includes? result "г. Иркутск"))
    (is (str/includes? result "-20°C"))
    (is (str/includes? result "мм.рт.ст."))
    (is (str/includes? result "м/с"))
    (is (str/includes? result "/info uiii  ^495 м"))))

(deftest format-station-info-temperature-trend
  (testing "rising trend"
    (let [result (fmt/format-station-info
                  {:st "x" :title "X" :last {:t 5.0 :t_delta 1.5}})]
      (is (str/includes? result "↑"))))

  (testing "falling trend"
    (let [result (fmt/format-station-info
                  {:st "x" :title "X" :last {:t 5.0 :t_delta -1.5}})]
      (is (str/includes? result "↓"))))

  (testing "stable — no arrow"
    (let [result (fmt/format-station-info
                  {:st "x" :title "X" :last {:t 5.0 :t_delta 0.5}})]
      (is (not (str/includes? result "↑")))
      (is (not (str/includes? result "↓"))))))

(deftest format-station-info-no-weather
  (let [result (fmt/format-station-info {:st "abc" :title "Тест" :last nil})]
    (is (str/includes? result "🔹 Тест"))
    (is (str/includes? result "/info abc"))))


;; --- format-days-of-week ---

(deftest format-days-of-week-single
  (is (= "пн" (fmt/format-days-of-week 1)))
  (is (= "вс" (fmt/format-days-of-week 64))))

(deftest format-days-of-week-workdays
  (is (= "пн,вт,ср,чт,пт" (fmt/format-days-of-week 31))))

(deftest format-days-of-week-weekend
  (is (= "сб,вс" (fmt/format-days-of-week 96))))

(deftest format-days-of-week-all
  (is (= "пн,вт,ср,чт,пт,сб,вс" (fmt/format-days-of-week 127))))


;; --- format-time ---

(deftest format-time-passthrough
  (is (= "08:00" (fmt/format-time "08:00")))
  (is (= "23:59" (fmt/format-time "23:59"))))
