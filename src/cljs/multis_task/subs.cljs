(ns multis-task.subs
  (:require
   [re-frame.core :as re-frame]
   [multis-task.util.form-events :as form-events]))

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
 ::erc20-token-name
 (fn [db]
   (get-in db [:token-stream-form :erc20-token-name] "Input your ERC20 address")))

(re-frame/reg-sub
 ::erc20-token-addr
 (fn [db]
   (get-in db [:token-stream-form :erc20-token-addr])))

(re-frame/reg-sub
 ::authorize-sablier-visible
 (fn [db]
   (not (get-in db (form-events/to-db-error-path :token-stream-form [:erc20-token-addr])))))

(re-frame/reg-sub
 ::sablier-stream-activation-visible
 (fn [db]
   false))
