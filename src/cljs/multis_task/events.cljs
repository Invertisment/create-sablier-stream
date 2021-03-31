(ns multis-task.events
  (:require
   [re-frame.core :as re-frame]
   [multis-task.db :as db]
   [multis-task.config :as config]
   [multis-task.interceptors :refer [interceptors]]))

(re-frame/reg-event-db
 ::initialize-db
 interceptors
 (fn [_ _]
   db/default-db))

(defn change-loader-timer [db op]
  (update db :loader-counter op 1))
#_(change-loader-timer {:loader-counter 0} +)

(re-frame/reg-event-db
 ::increase-loader-counter
 interceptors
 (fn [db [_]]
   (change-loader-timer db +)))

(re-frame/reg-event-db
 ::decrease-loader-counter
 interceptors
 (fn [db [_]]
   (change-loader-timer db -)))

(re-frame/reg-event-db
 :notify-error
 interceptors
 (fn [_ [_ error]]
   (println "[DISPLAY USER ERROR]" error)))

(re-frame/reg-event-db
 ::navigate
 interceptors
 (fn [db [_ to-route]]
   (assoc db :route to-route)))
