(ns meteomax.app.maxapi
  "https://dev.max.ru"
  (:require [org.httpkit.client :as http]
            [jsonista.core :as json]
            [taoensso.telemere :refer [log!]]))


(def ^:private api-base "https://platform-api.max.ru")


(defn- make-url [endpoint]
  (str api-base endpoint))


(defn- request-opts [token]
  {:headers {"Authorization" token
             "Content-Type" "application/json"}
   :timeout 10000})


(defn- http-call [http-method url opts params]
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
  "Like request, but returns {:ok true} or {:ok false :error-code N}."
  [http-method endpoint token params query-params]
  (let [url  (make-url endpoint)
        opts (cond-> (request-opts token)
               query-params (assoc :query-params query-params))]
    (log! {:level :debug
           :id    :maxapi/request
           :data  {:method http-method :endpoint endpoint :params params}})
    (try
      (let [{:keys [status body]} @(http-call http-method url opts params)
            parsed (try (json/read-value body json/keyword-keys-object-mapper)
                        (catch Exception _ nil))]
        (if (= 200 status)
          {:ok true}
          (do
            (log! {:level :warn
                   :id    :maxapi/response-error
                   :msg   "MAX API error"
                   :data  {:status status :body body}})
            {:ok false :error-code (:code parsed)})))
      (catch Exception e
        (log! {:level :error
               :id    :maxapi/request-exception
               :msg   "MAX API exception"
               :data  {:method http-method :endpoint endpoint :error (ex-message e)}})
        {:ok false :error-code :exception}))))


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


(defn upload-file [token file-path]
  (request :post "/uploads" token {:source file-path}))
