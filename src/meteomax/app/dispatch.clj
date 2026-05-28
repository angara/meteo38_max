(ns meteomax.app.dispatch
  "Update router - routes updates to handlers by type and chat."
  (:require [clojure.string :as str]
            [taoensso.telemere :refer [log!]]))

(defn route-update
  "Route MAX update to appropriate handler.
   
   Parameters:
   - update: MAX update map
   - handlers: Map of handler functions
   
   Returns: Handler function to call"
  [update handlers]
  (let [message (:message update)
        callback (:callback_query update)]
    (cond
      ;; Handle callback queries
      callback
      (if-let [handler (:callback handlers)]
        (handler callback)
        (log! :warn {:msg "No callback handler"}))
      
      ;; Handle messages
      message
      (let [^String text (:text message)
            location (:location message)]
        (cond
          ;; Location message
          location
          (if-let [handler (:location handlers)]
            (handler message)
            (log! :warn {:msg "No location handler"}))
          
          ;; Text message - check for commands
          text
          (let [command (if (str/starts-with? text "/")
                          (keyword (subs text 1 (or (str/index-of text " ")
                                                    (count text))))
                          :text)
                handler (get handlers command (:text handlers))]
            (if handler
              (handler message)
              (log! :warn {:msg "No handler for command" :command command})))

          :else
          (log! :warn {:msg "Unknown message type"})))

      :else
      (log! :warn {:msg "Unknown update type" :update update}))))

(comment
  ;; Example usage:
  (def handlers
    {:start (fn [_msg] (println "Start command"))
     :help (fn [_msg] (println "Help command"))
     :text (fn [_msg] (println "Text message"))
     :location (fn [_msg] (println "Location received"))
     :callback (fn [_cb] (println "Callback query"))})
  
  (route-update {:message {:chat {:id "123"} :text "/start"}} handlers)
)
