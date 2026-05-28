(ns meteomax.app.maxapi
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
    :get    (http/get url (assoc opts :query-params params))
    :post   (http/post url (assoc opts :body (json/write-value-as-string params)))
    :put    (http/put url (assoc opts :body (json/write-value-as-string params)))
    :patch  (http/patch url (assoc opts :body (json/write-value-as-string params)))
    :delete (http/delete url (assoc opts :query-params params))))


(defn- request
  [http-method endpoint token params]
  (let [url (make-url endpoint)
        opts (request-opts token)]
    (log! {:level :debug
           :id :maxapi/request
           :msg "MAX API request"
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
        nil))))


(defn get-updates [token]
  (request :get "/updates" token {}))


(defn get-subscriptions [token]
  (request :get "/subscriptions" token {}))


(defn set-webhook [token url secret]
  (request :post "/subscriptions" token {:url url
                                         :secret secret
                                         :update_types []}))


(defn send-message [token chat-id text & {:keys [format attachments link]}]
  (let [params (cond-> {:chat_id chat-id :text text}
                 format (assoc :format (name format))
                 attachments (assoc :attachments attachments)
                 link (assoc :link link))]
    (request :post "/messages" token params)))


(defn send-location [token chat-id latitude longitude]
  (let [location-attachment {:type "location" :payload {:lat latitude :lon longitude}}]
    (request :post "/messages" token {:chat_id chat-id :text "" :attachments [location-attachment]})))


(defn answer-callback [token callback-query-id & {:keys [text show-alert]}]
  (let [params (cond-> {:callback_query_id callback-query-id}
                 text (assoc :text text)
                 show-alert (assoc :show_alert show-alert))]
    (request :post "/answers" token params)))


(defn get-me [token]
  (request :get "/me" token {}))


(defn upload-file [token file-path]
  (request :post "/uploads" token {:source file-path}))
