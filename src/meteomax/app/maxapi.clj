(ns meteomax.app.maxapi
  "https://dev.max.ru"
  (:require [org.httpkit.client :as http]
            [jsonista.core :as json]
            [taoensso.telemere :refer [log!]]))


(def ^:private api-base "https://platform-api.max.ru")

(def ^:private throttle-lock (Object.))
(def ^:private last-request-ms (atom 0))
(def ^:private min-interval-ms 20)

(defn- throttle! []
  (locking throttle-lock
    (let [wait (- min-interval-ms (- (System/currentTimeMillis) @last-request-ms))]
      (when (pos? wait)
        (Thread/sleep wait)))
    (reset! last-request-ms (System/currentTimeMillis))))


(defn- make-url [endpoint]
  (str api-base endpoint))


(defn- request-opts [token]
  {:headers {"Authorization" token
             "Content-Type" "application/json"}
   :timeout 10000})


(defn- http-call [http-method url opts params]
  (throttle!)
  (case http-method
    :get    (http/get url (update opts :query-params merge params))
    :post   (http/post url (assoc opts :body (json/write-value-as-string params)))
    :put    (http/put url (assoc opts :body (json/write-value-as-string params)))
    :patch  (http/patch url (assoc opts :body (json/write-value-as-string params)))
    :delete (http/delete url (update opts :query-params merge params))))


(defn- request
  ([http-method endpoint token params]
   (request http-method endpoint token params nil))
  ([http-method endpoint token params query-params]
   (let [url  (make-url endpoint)
         opts (cond-> (request-opts token)
                query-params (assoc :query-params query-params))]
    (log! {:level :debug
           :id :maxapi/request
           :data {:method http-method :endpoint endpoint :params params}})
    (try
      (let [{:keys [status body]} @(http-call http-method url opts params)]
        (if (= 200 status)
          (json/read-value body json/keyword-keys-object-mapper)
          (do
            (log! {:level :warn
                   :id :maxapi/response-error
                   :msg "MAX API error"
                   :data {:status status :body body}})
            nil)))
      (catch Exception e
        (log! {:level :error
               :id :maxapi/request-exception
               :msg "MAX API exception"
               :data {:method http-method :endpoint endpoint :error (ex-message e)}})
        nil)))))


(defn- request-result
  "Like request, but returns {:ok true} or {:ok false :error-code N}.
   Retries up to 2 times on HTTP 429 with exponential backoff (1s, 2s)."
  [http-method endpoint token params query-params]
  (let [url  (make-url endpoint)
        opts (cond-> (request-opts token)
               query-params (assoc :query-params query-params))]
    (log! {:level :debug
           :id    :maxapi/request-ok
           :data  {:method http-method :endpoint endpoint :params params}})
    (loop [attempts 3 delay-ms 1000]
      (let [result
            (try
              (let [{:keys [status body]} @(http-call http-method url opts params)
                    parsed (try (json/read-value body json/keyword-keys-object-mapper)
                                (catch Exception _ nil))]
                (cond
                  (= 200 status) {:ok true}
                  (= 429 status) {:ok false :error-code :rate-limited}
                  :else
                  (do
                    (log! {:level :warn
                           :id    :maxapi/response-error
                           :msg   "MAX API error"
                           :data  {:status status :body body}})
                    {:ok false :error-code (or (:code parsed) status)})))
              (catch Exception e
                (log! {:level :error
                       :id    :maxapi/request-exception
                       :msg   "MAX API exception"
                       :data  {:method http-method :endpoint endpoint :error (ex-message e)}})
                {:ok false :error-code :exception}))]
        (if (and (= :rate-limited (:error-code result)) (> attempts 1))
          (do
            (log! {:level :warn
                   :id    :maxapi/rate-limited
                   :msg   "Rate limited, retrying"
                   :data  {:delay-ms delay-ms :attempts-left (dec attempts)}})
            (Thread/sleep delay-ms)
            (recur (dec attempts) (* 2 delay-ms)))
          result)))))


(defn get-updates [token]
  (request :get "/updates" token {}))


(defn get-subscriptions [token]
  (request :get "/subscriptions" token {}))


(defn set-webhook [token url secret]
  (request :post "/subscriptions" token {:url url
                                         :secret secret
                                         :update_types []}))


(defn send-message [token chat-id text & {:keys [format attachments link]}]
  (let [body (cond-> {:text text}
               format (assoc :format (name format))
               attachments (assoc :attachments attachments)
               link (assoc :link link))]
    (request-result :post "/messages" token body {:chat_id chat-id})))


(defn send-location [token chat-id latitude longitude]
  (let [location-attachment {:type "location" :payload {:lat latitude :lon longitude}}]
    (request :post "/messages" token {:text "" :attachments [location-attachment]} {:chat_id chat-id})))


(defn answer-callback [token callback-id & {:keys [notification message]}]
  (let [body (cond-> {}
               notification (assoc :notification notification)
               message      (assoc :message message))]
    (request :post "/answers" token body {:callback_id callback-id})))


(defn delete-message [token message-id]
  (request :delete "/messages" token {} {:message_id message-id}))


(defn get-me [token]
  (request :get "/me" token {}))


(defn set-commands [token commands]
  (request :patch "/me" token {:commands commands}))


(defn upload-file [token file-path]
  (request :post "/uploads" token {:source file-path}))
