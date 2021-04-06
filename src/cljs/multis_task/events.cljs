(ns multis-task.events
  (:require
   [re-frame.core :as re-frame]
   [multis-task.db :as db]
   [multis-task.config :as config]
   [multis-task.interceptors :refer [interceptors]]
   [multis-task.util.validation :as validation]
   [clojure.string :as strs]
   [multis-task.subs :as subs]))

(re-frame/reg-event-db
 ::initialize-db
 interceptors
 (fn [_ _]
   db/default-db))

(defn change-loader-timer-fx [{:keys [db] :as cofx} op further-event further-event-args]
  (if further-event
    {:db (update db :loader-counter op 1)
     :dispatch (into further-event further-event-args)}
    {:db (update db :loader-counter op 1)}))
#_(change-loader-timer-fx {:db {:loader-counter 0}} + [:println] [[:arg1 :arg2]])

(re-frame/reg-event-fx
 ::increase-loader-counter
 interceptors
 (fn [cofx [_ further-event & further-event-args]]
   (change-loader-timer-fx cofx + further-event further-event-args)))

(re-frame/reg-event-fx
 ::decrease-loader-counter
 interceptors
 (fn [cofx [_ further-event & further-event-args]]
   (change-loader-timer-fx cofx - further-event further-event-args)))

(defn append-notification [db notif-type text]
  {:db (update db notif-type
               (fn [texts]
                 (conj
                  (remove #(= % text) texts)
                  text)))
   ::clear-notif-later {:notif-type notif-type
                        :text text}})

(re-frame/reg-event-fx
 :notify-error
 interceptors
 (fn [{:keys [db] :as cofx} [_ error]]
   (append-notification db :ui-errors error)))

(re-frame/reg-event-fx
 :notify-success
 interceptors
 (fn [{:keys [db] :as cofx} [_ text]]
   (append-notification db :ui-successes text)))

(defn clear-notif [db notif-type notif]
  (update db notif-type
          (fn [notifs]
            (remove #(= % notif) notifs))))

(re-frame/reg-event-db
 :clear-notif
 interceptors
 (fn [db [_ notif-type error]]
   (clear-notif db notif-type error)))

(re-frame/reg-fx
 ::clear-notif-later
 (fn [{:keys [notif-type text]}]
   (js/setTimeout
    #(re-frame/dispatch [:clear-notif notif-type text])
    7001)))

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
