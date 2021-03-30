(ns multis-task.metamask
  (:require
   [re-frame.core :as re-frame]
   [multis-task.util.async :as async-util]
   [day8.re-frame.async-flow-fx :as async-flow-fx]
   [multis-task.interceptors :refer [interceptors]]
   [ethers :as eth]))

;; ----- Coeffects -----

;;const provider = new ethers.providers.Web3Provider(.-ethereum window)

(re-frame/reg-cofx
 ::ethers-provider
 (fn [cofx _]
   (let [constructor (some-> ethers
                             (aget "providers")
                             (aget "Web3Provider"))
         eth-instance (.-ethereum js/window)]
     (if (and constructor eth-instance)
       (assoc cofx
              ::ethers-provider (constructor. eth-instance))
       cofx))))

;; ----- EFfects -----

(re-frame/reg-fx
 ::activate-metamask
 (fn [promise-args]
   (async-util/promise->dispatch
    ;; NOTE: The console says that (.enable ethereum) deprecated.
    ;; But it's what people do and it doesn't work without it.
    (.enable (.-ethereum js/window))
    promise-args)))

(re-frame/reg-fx
 ::retrieve-account-info
 (fn [[eth-provider promise-args]]
   (async-util/promise->dispatch
    (.listAccounts eth-provider)
    promise-args)))

(re-frame/reg-fx
 ::get-network-name
 (fn [[eth-provider promise-args]]
   (async-util/promise->dispatch
    (.getNetwork eth-provider)
    promise-args)))

;; ----- EVents -----

(re-frame/reg-event-db
 ::account-info-retrieved
 interceptors
 (fn [db [_ accounts]]
   (if accounts
     (assoc-in db [:metamask-data :chosen-account] (first accounts))
     db)))

(re-frame/reg-event-db
 ::get-network-name-success
 interceptors
 (fn [db [_ ethjs-network]]
   (if-let [net-name (ethjs-network "name")]
     (assoc-in db [:metamask-data :network-name] net-name)
     db)))

(re-frame/reg-event-db
 ::metamask-access-granted
 interceptors
 (fn [db _]
   db))

(re-frame/reg-event-fx
 ::request-metamask-access
 interceptors
 (fn [cofx _]
   {::activate-metamask
    {:on-success [::metamask-access-granted]
     :on-fail [:notify-error "Can't activate Metamask"]}}))

(re-frame/reg-event-fx
 ::gather-session-info
 (cons (re-frame/inject-cofx ::ethers-provider) interceptors)
 (fn [cofx _]
   {::retrieve-account-info [(::ethers-provider cofx)
                             {:on-success [::account-info-retrieved]
                              :on-fail [:notify-error "Can't retrieve account info from Metamask"]}]
    ::get-network-name [(::ethers-provider cofx)
                        {:on-success [::get-network-name-success]
                         :on-fail [:notify-error "Can't get network info from Metamask"]}]}))

;; ----- Pipelines -----

(defn set-up-metamask-flow [{:keys [on-success on-fail on-finally]}]
  {:first-dispatch [::request-metamask-access]
   :rules [{:when :seen?
            :events ::metamask-access-granted
            :dispatch [::gather-session-info]}
           {:when :seen-all-of?
            :events [::account-info-retrieved ::get-network-name-success]
            :dispatch-n (vec (remove nil? [on-success on-finally]))
            :halt true}
           {:when :seen-any-of?
            :events :notify-error
            :dispatch-n (vec (remove nil? [on-fail on-finally]))
            :halt? true}]})
