(ns meteomax.app.inbound
  "Inbound message/callback dispatcher."
  (:require [meteomax.app.command :as command]
            [meteomax.app.dispatch :as dispatch]
            [taoensso.telemere :as log]))

(defn make-handlers
  "Create handler map with config and db bindings."
  [config db]
  {:start (fn [msg]
            (let [chat (:chat msg)
                  chat-id (:id chat)
                  from (:from msg)]
              (command/handle-start config db chat-id
                                    (:username from)
                                    (:first_name from))))
   :help (fn [msg]
           (command/handle-help config (:id (:chat msg))))
   :near (fn [msg]
           (let [text (:text msg)
                 args (rest (clojure.string/split text #" "))]
             (command/handle-near config db (:id (:chat msg)) args)))
   :active (fn [msg]
             (let [text (:text msg)
                   args (rest (clojure.string/split text #" "))]
               (command/handle-active config db (:id (:chat msg)) args)))
   :favs (fn [msg]
           (command/handle-favs config db (:id (:chat msg))))
   :info (fn [msg]
           (let [text (:text msg)
                 args (rest (clojure.string/split text #" "))]
             (command/handle-info config db (:id (:chat msg)) args)))
   :subs (fn [msg]
           (command/handle-subs config db (:id (:chat msg))))
   :sub (fn [msg]
          (command/handle-sub config (:id (:chat msg))
                              (rest (clojure.string/split (:text msg) #" "))))
   :text (fn [msg]
           (command/handle-text config (:id (:chat msg)) (:text msg)))
   :location (fn [msg]
               (let [loc (:location msg)
                     chat-id (:id (:chat msg))]
                 (command/handle-location config db chat-id
                                          (:latitude loc)
                                          (:longitude loc))))
   :callback (fn [cb]
               (log/log! :info {:msg "Callback received" :data (:data cb)})
               ;; TODO: Implement callback handling
               )})

(defn handle-update
  "Process incoming update from MAX.
   
   Parameters:
   - config: Application config
   - db: Database connection
   - update: Update map from MAX API"
  [config db update]
  (let [handlers (make-handlers config db)]
    (dispatch/route-update update handlers)))

(comment
  ;; Example:
  (handle-update config db
    {:message {:chat {:id "123"}
               :from {:username "test" :first_name "Test"}
               :text "/start"}})
)
