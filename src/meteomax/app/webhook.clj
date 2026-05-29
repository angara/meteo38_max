(ns meteomax.app.webhook
  (:require [clojure.string :as str]
            [jsonista.core :as json]
            [meteomax.app.command :as command]
            [meteomax.app.maxapi :as maxapi]
            [meteomax.db.pg :as pg]
            [meteomax.lib.random :as random]
            [mount.core :as mount]
            [org.httpkit.server :as http-server]
            [taoensso.telemere :refer [log!]]))


(defn- webhook-path [config]
  (:webhook-path config))


(defn- header-secret [req]
  (some-> req :headers (get "x-max-bot-api-secret")))


(defn valid-secret? [expected-secret req]
  (= expected-secret (header-secret req)))


(defn- parse-body [req]
  (let [body (slurp (:body req))]
    (json/read-value body json/keyword-keys-object-mapper)))


(defn- response [status body]
  {:status status
   :headers {"Content-Type" "text/plain; charset=utf-8"}
   :body body})


(defn- attachment-by-type [message attachment-type]
  (->> (-> message :body :attachments)
       (some #(when (= attachment-type (:type %)) %))))


(defn- handle-command [config db chat-id user text]
  (case (-> text str/trim str/lower-case)
    "/start" (do (command/handle-start config db chat-id user) true)
    "/help"  (do (command/handle-help config db chat-id) true)
    "/favs"  (do (command/handle-favs config db chat-id) true)
    "."      (do (command/handle-favs config db chat-id) true)
    "/subs"  (do (command/handle-subs config db chat-id) true)
    ","      (do (command/handle-subs config db chat-id) true)
    nil))


(defn- handle-location-message [config db chat-id message]
  (if-let [location (attachment-by-type message "location")]
    (command/handle-location config db chat-id location)
    (do
      (log! {:level :warn
             :id    :webhook/location-not-found
             :msg   "Location attachment missing payload"
             :data  {:message message}})
      nil)))


(defn- handle-message-created
  [config db update]
  (let [message (:message update)
        chat-id (str (or (:chat_id update) (some-> message :recipient :chat_id)))
        text    (some-> message :body :text)
        user    (:sender message)]
    (cond
      (seq text)
      (or
       (handle-command config db chat-id user text)
       (command/handle-text config db chat-id text))

      (attachment-by-type message "location")
      (handle-location-message config db chat-id message)

      :else
      (log! {:level :info
             :id :webhook/update-ignored
             :msg "Ignoring unsupported message_created payload"
             :data {:chat-id chat-id}}))))


(defn- handle-message-callback [config db update]
  (let [callback-id (some-> update :callback :callback_id)
        payload     (some-> update :callback :payload)
        chat-id     (str (some-> update :message :recipient :chat_id))]
    (log! {:id :webhook/callback-received
           :msg "Callback update received"
           :data {:chat-id chat-id :payload payload}})
    (cond
      (str/starts-with? payload "fav:toggle:")
      (command/handle-fav-toggle config db chat-id callback-id
                                 (subs payload (count "fav:toggle:")))

      :else
      (log! {:id :webhook/callback-unhandled
             :msg "Unhandled callback payload"
             :data {:payload payload}}))))


(defn- handle-bot-started [config db update]
  (command/handle-start config db (str (:chat_id update)) (:user update)))


(defn handle-update [config db update]
  (log! {:id :webhook/update-received :data {:update-type (:update_type update)}})
  (case (:update_type update)
    "message_created"  (handle-message-created config db update)
    "message_callback" (handle-message-callback config db update)
    "bot_started"      (handle-bot-started config db update)
    (log! {:id :webhook/update-ignored :data {:update-type (:update_type update)}})))


(defn webhook-handler
  [config db secret req]
  (try
    (let [path (webhook-path config)]
      (cond
        (not= path (:uri req))
        (response 404 "Not found")

        (not= :post (:request-method req))
        (response 405 "Method not allowed")

        (not (valid-secret? secret req))
        (do
          (log! {:level :warn
                 :id :webhook/secret-mismatch
                 :msg "Rejecting webhook request with invalid secret"})
          (response 403 "Forbidden"))

        :else
        (do
          (handle-update config db (parse-body req))
          (response 200 "OK"))))
    (catch com.fasterxml.jackson.core.JsonParseException e
      (log! {:level :warn
             :id :webhook/parse-error
             :msg "Failed to parse webhook payload"
             :data {:error (ex-message e)}})
      (response 400 "Bad request"))
    (catch Exception e
      (log! {:level :error
             :id :webhook/handler-error
             :msg "Webhook handler error"
             :data {:error (ex-message e)}})
      (response 500 "Internal error"))))


(defn start-webhook-server
  [config db secret]
  (let [bind (:webhook-bind config "localhost")
        port (:webhook-port config 8005)
        path (webhook-path config)]
    (log! {:level :info
           :id :webhook/server-start
           :msg "Starting webhook server"
           :data {:bind bind :port port :path path}})
    (let [server (http-server/run-server
                  (fn [req]
                    (webhook-handler config db secret req))
                  {:ip bind
                   :port port
                   :thread 4})]
      {:server server
       :bind bind
       :port port
       :path path})))


(defn stop-webhook-server
  [webhook-server]
  (log! {:level :info
         :id :webhook/server-stop
         :msg "Stopping webhook server"})
  (when-let [server (:server webhook-server)]
    (server)))


(defn register-webhook!
  [config secret]
  (let [token (:max-api-token config)
        url   (:webhook-url config)
        resp  (maxapi/set-webhook token url secret)]
    (if (:success resp)
      (do
        (log! {:level :info
               :id :webhook/subscription-set
               :msg "Webhook subscription registered"
               :data {:url url}})
        resp)
      (do
        (log! {:level :error
               :id :webhook/subscription-failed
               :msg "Failed to register webhook subscription"
               :data {:url url :response resp}})
        (throw (ex-info "Failed to register webhook subscription"
                        {:url url :response resp}))))))


(mount/defstate webhook-secret
  :start (random/webhook-secret))


(mount/defstate webhook-endpoint
  :start (start-webhook-server (mount/args) pg/conn webhook-secret)
  :stop  (stop-webhook-server webhook-endpoint))


(mount/defstate webhook-subscription
  :start (register-webhook! (mount/args) webhook-secret))
