(ns multis-task.metamask
  (:require
   [re-frame.core :as re-frame]
   [multis-task.util.async :as async-util]
   [day8.re-frame.async-flow-fx :as async-flow-fx]
   [multis-task.interceptors :refer [interceptors]]
   [ethers :as eth]
   [multis-task.db :as db]))

;; ----- Coeffects -----

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
 ::add-accounts-changed-callback
 (fn [on-accounts-changed]
   (async-util/eth-event->dispatch "accountsChanged" on-accounts-changed)))

(re-frame/reg-fx
 ::add-chain-changed-callback
 (fn [on-chain-changed]
   (async-util/eth-event->dispatch "chainChanged" on-chain-changed)))

(re-frame/reg-fx
 ::add-chain-disconnected-callback
 (fn [on-chain-dcd]
   (async-util/eth-event->dispatch "disconnect" on-chain-dcd)))

(re-frame/reg-fx
 ::retrieve-account-info
 (fn [promise-args]
   (async-util/promise->dispatch
    (.request js/ethereum (clj->js {:method "eth_requestAccounts"}))
    promise-args)))

(re-frame/reg-fx
 ::get-network-name
 (fn [[eth-provider promise-args]]
   (async-util/promise->dispatch
    (.getNetwork eth-provider)
    promise-args)))

(defn set-up-get-network-name-effect [ethers-provider]
  [ethers-provider
   {:on-success [::network-name-retrieved]
    :on-fail [:notify-error "Can't get network info from Metamask"]}])

;; ----- EVents -----

(re-frame/reg-event-db
 ::account-info-retrieved
 interceptors
 (fn [db [_ accounts]]
   (if accounts
     (assoc-in db [:metamask-data :chosen-account] (first accounts))
     (assoc db :metamask-data db/default-metamask-data-state))))

(re-frame/reg-event-fx
 ::chain-changed
 (cons (re-frame/inject-cofx ::ethers-provider) interceptors)
 (fn [cofx _]
   {::get-network-name (set-up-get-network-name-effect (::ethers-provider cofx))}))

(re-frame/reg-event-fx
 ::chain-disconnected
 interceptors
 (fn [{:keys [db] :as cofx} _]
   {:db (assoc db :metamask-data db/default-metamask-data-state)
    :dispatch [:notify-error "Chain disconnected. You may need to reload the page."]}))

(re-frame/reg-event-db
 ::network-name-retrieved
 interceptors
 (fn [db [_ ethjs-network]]
   (if-let [net-name (ethjs-network "name")]
     (assoc-in db [:metamask-data :network-name] net-name)
     db)))

(re-frame/reg-event-fx
 ::init
 (cons (re-frame/inject-cofx ::ethers-provider) interceptors)
 (fn [{:keys [db] :as cofx} _]
   (if (get-in db [:metamask-data :initialized])
     {:dispatch [::async-flow-fx/notify ::init-skipped]}
     {::add-accounts-changed-callback [::account-info-retrieved]
      ::add-chain-changed-callback [::chain-changed]
      ::add-chain-disconnected-callback [::chain-disconnected]
      :db (assoc-in db [:metamask-data :initialized] true)
      :dispatch [::async-flow-fx/notify ::init-done]})))

(re-frame/reg-event-fx
 ::gather-session-info
 (cons (re-frame/inject-cofx ::ethers-provider) interceptors)
 (fn [cofx _]
   {::retrieve-account-info {:on-success [::account-info-retrieved]
                             :on-fail [:notify-error "Can't retrieve account info from Metamask"]}
    ::get-network-name (set-up-get-network-name-effect (::ethers-provider cofx))}))

;; ----- Pipelines -----

(defn set-up-metamask-flow [{:keys [on-success on-fail on-finally]}]
  {:first-dispatch [::init]
   :rules [{:when :seen-any-of?
            :events [[::async-flow-fx/notify ::init-done]
                     [::async-flow-fx/notify ::init-skipped]]
            :dispatch [::gather-session-info]}
           {:when :seen-all-of?
            :events [::account-info-retrieved ::network-name-retrieved]
            :dispatch-n (remove nil? [on-success on-finally])
            :halt true}
           {:when :seen-any-of?
            :events :notify-error
            :dispatch-n (remove nil? [on-fail on-finally])
            :halt? true}]})
