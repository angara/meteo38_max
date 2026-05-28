(ns meteomax.app.webhook-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as json]
            [meteomax.app.webhook :as webhook]))


(defn- request
  ([body]
   (request body {}))
  ([body headers]
   {:uri "/webhook"
    :request-method :post
    :headers headers
    :body (java.io.ByteArrayInputStream. (.getBytes body "UTF-8"))}))


(def ^:private webhook-config
  {:webhook-path "/webhook"})


(deftest valid-secret-checks-header
  (is (true? (webhook/valid-secret? "secret" (request "{}" {"x-max-bot-api-secret" "secret"}))))
  (is (false? (webhook/valid-secret? "secret" (request "{}" {"x-max-bot-api-secret" "other"})))))


(deftest webhook-handler-rejects-invalid-requests
  (testing "wrong path"
    (is (= 404 (:status (webhook/webhook-handler webhook-config nil "secret" (assoc (request "{}") :uri "/nope"))))))
  (testing "wrong method"
    (is (= 405 (:status (webhook/webhook-handler webhook-config nil "secret" (assoc (request "{}") :request-method :get))))))
  (testing "wrong secret"
    (is (= 403 (:status (webhook/webhook-handler webhook-config nil "secret" (request "{}" {"x-max-bot-api-secret" "other"}))))))
  (testing "bad json"
    (is (= 400 (:status (webhook/webhook-handler webhook-config nil "secret" (request "{" {"x-max-bot-api-secret" "secret"})))))))


(deftest webhook-handler-returns-200-for-known-and-unknown-updates
  (with-redefs [webhook/handle-update (fn [_config _db _update] :ok)]
    (is (= 200
           (:status (webhook/webhook-handler webhook-config nil "secret"
                                             (request (json/write-value-as-string {:update_type "custom"})
                                                      {"x-max-bot-api-secret" "secret"})))))))
