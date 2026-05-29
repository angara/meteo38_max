(ns meteomax.db.subs-test
  (:require [clojure.test :refer [deftest is]]
            [meteomax.db.subs :as subs]
            [pg.core :as pg]))


(deftest create-subscription-uses-parameterized-query
  (let [calls (atom [])]
    (with-redefs [pg/execute (fn [db sql opts]
                               (swap! calls conj [db sql opts])
                               [{:chat_id 1 :station_name "uiii" :time_str "08:00" :days_of_week 127}])]
      (is (= {:chat_id 1 :station_name "uiii" :time_str "08:00" :days_of_week 127}
             (subs/create-subscription! :db 1 "uiii" "08:00" 127)))
      (is (= [1 "uiii" "08:00" 127]
             (get-in (first @calls) [2 :params]))))))
