(ns multis-task.subs
  (:require
   [re-frame.core :as re-frame]))

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

(re-frame/reg-sub
 ::metamask-data
 (fn [db query-v]
   (get db :metamask-data)))

(re-frame/reg-sub
 ::chosen-network-name
 (fn [query-v]
   [(re-frame/subscribe [::metamask-data])])
 (fn [[metamask-data] query-v]
   (when (:initialized metamask-data)
     (get metamask-data :network-name "unknown"))))

(re-frame/reg-sub
 ::chosen-acc-addr
 (fn [query-v]
   [(re-frame/subscribe [::metamask-data])])
 (fn [[metamask-data] query-v]
   (when (:initialized metamask-data)
     (get metamask-data :chosen-account "unknown"))))

(re-frame/reg-sub
 ::route
 (fn [db]
   (:route db)))

(re-frame/reg-sub
 ::ui-errors
 (fn [db]
   (:ui-errors db)))

(re-frame/reg-sub
 ::erc20-token-name-input
 (fn [db]
   (get-in db [:token-stream-form :erc20-token-input :name] "Input your ERC20 address")))

(re-frame/reg-sub
 ::erc20-token-addr-input
 (fn [db]
   (get-in db [:token-stream-form :erc20-token-input :addr])))

(re-frame/reg-sub
 ::authorize-sablier-visible
 (fn [db]
   (boolean (get-in db [:token-stream-form :erc20-token-input :name]))))
