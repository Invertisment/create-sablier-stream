(ns multis-task.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::loader-counter
 (fn [db]
   (:loader-counter db)))

(re-frame/reg-sub
 ::loading
 (fn [query-v]
   [(re-frame/subscribe [::loader-counter])])
 (fn [[loader-counter] query-v]
   (> loader-counter 0)))
