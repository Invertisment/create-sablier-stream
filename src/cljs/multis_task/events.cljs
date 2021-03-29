(ns multis-task.events
  (:require
   [re-frame.core :as re-frame]
   [multis-task.db :as db]
   [multis-task.config :as config]
   [multis-task.metamask :as metamask]
   [day8.re-frame.async-flow-fx :as async-flow-fx]
   [multis-task.interceptors :refer [interceptors]]))

(re-frame/reg-event-db
 ::initialize-db
 interceptors
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::increase-loader-counter
 interceptors
 (fn [db [_]]
   (update db :loader-counter #(+ (or % 0) 1))))

(re-frame/reg-event-db
 ::decrease-loader-counter
 interceptors
 (fn [db [_]]
   (update db :loader-counter #(- (or % 0) 1))))

(re-frame/reg-event-db
 :notify-error
 interceptors
 (fn [_ [_ error]]
   (println "[DISPLAY USER ERROR]" error)))

(re-frame/reg-event-fx
 ::activate-metamask
 interceptors
 (fn [cofx]
   {:async-flow (metamask/set-up-metamask-flow {:on-finally [::decrease-loader-counter]})
    :dispatch [::increase-loader-counter]}))
