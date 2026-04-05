(ns mlib.max.botapi
  "MAX Bot API client.
   
   Based on MAX messenger API documentation at https://dev.max.ru/docs"
  (:require [org.httpkit.client :as http]
            [jsonista.core :as json]
            [taoensso.telemere :as log]))

(def ^:private api-base "https://platform-api.max.ru")

(defn- make-url
  "Build API URL from endpoint path."
  [endpoint]
  (str api-base endpoint))

(defn- request
  "Make HTTP request to MAX API.
   
   Parameters:
   - method: HTTP method (get, post, put, patch, delete)
   - endpoint: API endpoint path (e.g. \"/messages\")
   - token: Bot authentication token
   - params: Request parameters"
  [http-method endpoint token params]
  (let [url (make-url endpoint)
        headers {"Authorization" token
                 "Content-Type" "application/json"}
        opts {:headers headers
              :timeout 10000}]
    (log/log! :debug {:msg "MAX API request" :method http-method :endpoint endpoint :params params})
    (try
      (let [response (case http-method
                       :get (http/get url (assoc opts :query-params params))
                       :post (http/post url (assoc opts :body (json/write-value-as-string params)))
                       :put (http/put url (assoc opts :body (json/write-value-as-string params)))
                       :patch (http/patch url (assoc opts :body (json/write-value-as-string params)))
                       :delete (http/delete url (assoc opts :query-params params)))]
            {:keys [status body]} @response]
        (if (= 200 status)
          (json/read-value body json/keyword-keys-object-mapper)
          (do
            (log/log! :warn {:msg "MAX API error" :status status :body body})
            nil)))
      (catch Exception e
        (log/log! :error {:msg "MAX API exception" :method http-method :endpoint endpoint :error (.getMessage e)})
        nil))))

(defn get-updates
  "Get updates from MAX messenger using Long Polling.
   
   Returns: Array of Update objects"
  [token]
  (request :get "/updates" token {}))

(defn send-message
  "Send message to chat.
   
   Parameters:
   - token: Bot token
   - chat-id: Unique identifier for the target chat
   - text: Text of the message to be sent
   - options: Optional parameters
     - :format - Message format (:markdown or :html)
     - :attachments - Array of attachment objects
     - :link - Link attachment for replies
   
   Returns: Message object"
  [token chat-id text & {:keys [format attachments link]}]
  (let [params (cond-> {:chat_id chat-id
                        :text text}
                       format (assoc :format (name format))
                       attachments (assoc :attachments attachments)
                       link (assoc :link link))]
    (request :post "/messages" token params)))

(defn send-location
  "Send location to chat using attachment.
   
   Parameters:
   - token: Bot token
   - chat-id: Unique identifier for the target chat
   - latitude: Latitude of the location
   - longitude: Longitude of the location
   
   Returns: Message object"
  [token chat-id latitude longitude]
  (let [location-attachment {:type "location"
                             :payload {:lat latitude
                                       :lon longitude}}]
    (request :post "/messages" token {:chat_id chat-id
                                      :text ""
                                      :attachments [location-attachment]})))

(defn answer-callback
  "Answer to a callback query.
   
   Parameters:
   - token: Bot token
   - callback-query-id: Unique identifier for the query to be answered
   - text: Text of the notification (optional)
   - show-alert: Show alert instead of notification (optional)
   
   Returns: Success status"
  [token callback-query-id & {:keys [text show-alert]}]
  (let [params (cond-> {:callback_query_id callback-query-id}
                       text (assoc :text text)
                       show-alert (assoc :show_alert show-alert))]
    (request :post "/answers" token params)))

(defn get-me
  "Get information about the bot.
   
   Returns: User object with bot information"
  [token]
  (request :get "/me" token {}))

(defn upload-file
  "Upload file to MAX servers.
   
   Parameters:
   - token: Bot token
   - file-path: Path to file or file data
   
   Returns: Attachment token"
  [token file-path]
  (request :post "/uploads" token {:source file-path}))

(comment
  ;; Example usage:
  (def token "your-bot-token")
  
  ;; Get bot info
  (get-me token)
  ;; => {:user_id 123 :name "Weather Bot" :username "weather_bot" ...}
  
  ;; Get updates (Long Polling)
  (get-updates token)
  ;; => [{:update_type "message_created" :chat_id "123" :message {...}}]
  
  ;; Send text message
  (send-message token "123" "Hello, World!")
  
  ;; Send message with markdown formatting
  (send-message token "123" "**Bold** and *italic*" :format :markdown)
  
  ;; Send message with HTML formatting
  (send-message token "123" "<b>Bold</b> and <i>italic</i>" :format :html)
  
  ;; Send location
  (send-location token "123" 52.267288 104.366972)
  
  ;; Answer callback query
  (answer-callback token "cb123" :text "Processing...")
  
  ;; Answer callback with alert
  (answer-callback token "cb123" :text "Error!" :show-alert true)
  
  ;; Upload file
  (upload-file token "/path/to/image.png")
  ;; => {:token "file-token-123"}
)
