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

(re-frame/reg-event-fx
 :notify-error
 interceptors
 (fn [{:keys [db] :as cofx} [_ error]]
   {:db (update db :ui-errors
                (fn [errors]
                  (conj
                   (remove #(= % error) errors)
                   error)))
    ::clear-error-later error}))

(re-frame/reg-event-db
 :clear-error
 interceptors
 (fn [db [_ error]]
   (update db :ui-errors
           (fn [errors]
             (remove #(= % error) errors)))))

(re-frame/reg-fx
 ::clear-error-later
 (fn [error]
   (js/setTimeout
    #(re-frame/dispatch [:clear-error error])
    3000)))

(re-frame/reg-event-db
 ::navigate
 interceptors
 (fn [db [_ to-route]]
   (assoc db :route to-route)))

(re-frame/reg-event-fx
 :println
 (fn [_ [_ & data]]
   (apply println data)))

(re-frame/reg-event-fx
 :console-log
 (fn [_ [_ & data]]
   (apply (.-log js/console) data)))

(re-frame/reg-event-fx
 :clj->js
 (fn [_ [_ notify-callback data]]
   {:dispatch (conj notify-callback (clj->js data))}))

(re-frame/reg-event-fx
 :js->clj
 (fn [_ [_ notify-callback data]]
   {:dispatch (conj notify-callback (js->clj data))}))
