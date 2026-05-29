(ns meteomax.db.users-test
  (:require [clojure.test :refer [deftest is]]
            [meteomax.db.users :as users]
            [pg.core :as pg]))


(deftest get-favorites-defaults-to-empty-vector
  (with-redefs [pg/execute (fn [_db _sql _opts] [])]
    (is (= [] (users/get-favorites nil 1)))))


(deftest add-favorite-uses-parameterized-query
  (let [calls (atom [])]
    (with-redefs [pg/execute (fn [db sql opts]
                               (swap! calls conj [db sql opts])
                               [{:favs ["uiii"]}])]
      (users/add-favorite! :db 42 "uiii")
      (is (= :db (ffirst @calls)))
      (is (= [42 ["uiii"]] (get-in (first @calls) [2 :params]))))))


(deftest ensure-user-stores-userinfo
  (let [calls (atom [])]
    (with-redefs [pg/execute (fn [db sql opts]
                               (swap! calls conj [db sql opts])
                               [{:chat_id "1" :userinfo {:name "Иван"}}])]
      (users/ensure-user! :db "1" {:name "Иван" :username "ivan"})
      (is (= :db (ffirst @calls)))
      (is (= ["1" {:name "Иван" :username "ivan"}]
             (get-in (first @calls) [2 :params]))))))
