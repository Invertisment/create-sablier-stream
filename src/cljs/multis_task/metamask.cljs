(ns multis-task.metamask
  (:require
   [re-frame.core :as re-frame]
   ["web3-eth" :as eth]
   [multis-task.util.async :as async-util]
   [day8.re-frame.async-flow-fx :as async-flow-fx]
   [multis-task.interceptors :refer [interceptors]]))

;; ----- Coeffects -----

(re-frame/reg-cofx
 ::web3-eth
 (fn [cofx _]
   (if-let [provider eth/givenProvider]
     (assoc cofx
            ::web3-eth (eth. provider))
     cofx)))

;; ----- EFfects -----

(re-frame/reg-fx
 ::retrieve-account-info
 (fn [[web3-eth promise-args]]
   (async-util/promise->dispatch
    (.requestAccounts web3-eth)
    promise-args)))

(re-frame/reg-fx
 ::get-network-type
 (fn [[web3-eth promise-args]]
   (async-util/promise->dispatch
    (.getNetworkType (.-net web3-eth))
    promise-args)))

;; ----- EVents -----

(re-frame/reg-event-db
 ::account-info-retrieved
 interceptors
 (fn [db [_ accounts]]
   (if accounts
     (assoc-in db [:metamask-data :accounts] (first accounts))
     db)))

(re-frame/reg-event-db
 ::get-network-type-success
 interceptors
 (fn [db [_ net-type]]
   (if net-type
     (assoc-in db [:metamask-data :network-type] net-type)
     db)))

(re-frame/reg-event-fx
 ::gather-session-info
 [(re-frame/inject-cofx ::web3-eth)]
 (fn [cofx _]
   {::retrieve-account-info [(::web3-eth cofx)
                             {:on-success [::account-info-retrieved]
                              :on-fail [:notify-error "Can't retrieve account info from Metamask"]}]
    ::get-network-type [(::web3-eth cofx)
                        {:on-success [::get-network-type-success]
                         :on-fail [:notify-error "Can't get network info from Metamask"]}]}))

;; ----- Pipelines -----

(defn set-up-metamask-flow [{:keys [on-success on-fail on-finally]}]
  {:first-dispatch [::gather-session-info]
   :rules [{:when :seen-all-of?
            :events [::account-info-retrieved ::get-network-type-success]
            :dispatch-n (vec (remove nil? [on-success on-finally]))
            :halt true}
           {:when :seen-any-of?
            :events :notify-error
            :dispatch-n (vec (remove nil? [on-fail on-finally]))
            :halt? true}]})
