(ns multis-task.contracts
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            ["erc-20-abi" :as erc-20-abi]
            [ethers :as ethers]
            [multis-task.util.async :as async]
            [multis-task.events :as events]
            [day8.re-frame.async-flow-fx :as async-flow-fx]))

(re-frame/reg-event-fx
 :notify-http-failure
 (fn [_ [_ notify-callback data]]
   {:dispatch (conj notify-callback (or (:last-error data)
                                        (:debug-message data)
                                        (:status-text data)
                                        (:status data)))}))

(defn- mk-fetch-abi-event [_ [_ abi-type on-success on-fail]]
  (case abi-type
    :sablier {:http-xhrio {:method     :get
                           :uri        "https://raw.githubusercontent.com/sablierhq/sablier-abis/v1/Sablier.json"
                           :timeout    2000
                           :response-format (ajax/json-response-format)
                           :on-success [::events/decrease-loader-counter [:clj->js on-success]]
                           :on-failure [::events/decrease-loader-counter [:notify-http-failure on-fail]]}
              :dispatch [::events/increase-loader-counter]}
    :erc20 {:dispatch (conj on-success erc-20-abi)}
    {:dispatch (conj on-fail "Unknown token")}))
#_(mk-fetch-abi-event :cofx [:e :sablier [:on-success] [:on-fail]])
#_(mk-fetch-abi-event :cofx [:e :erc20 [:on-success] [:on-fail]])
#_(mk-fetch-abi-event :cofx [:e :other [:on-success] [:on-fail]])

(re-frame/reg-event-fx
 ::fetch-abi
 mk-fetch-abi-event)

(defn is-addr? [addr]
  (.isAddress (.-utils ethers) addr))

(defn- call-read-only-method-fx [[contract-addr abi [contract-method contract-args] on-success on-fail]]
  (if (is-addr? contract-addr)
    (let [contract (ethers/Contract. contract-addr abi (.getSigner (multis-task.metamask/mk-ethers-provider!)))]
      (async/promise->dispatch
       (apply (aget contract contract-method) contract-args)
       {:on-success on-success
        :on-fail on-fail}))
    (re-frame/dispatch (conj on-fail "Bad contract address")))
  {})
#_(call-read-only-method-fx
   ["0xfab46e002bbf0b4509813474841e0716e6730136"
    #_"0x1111111111111111111111111111111111111111"
    erc-20-abi
    ["name"]
    [:println :ok]
    [:println :error]])

(re-frame/reg-fx
 ::call-method
 call-read-only-method-fx)

(re-frame/reg-event-db
 ::fetch-erc20-name-success
 (fn [db [_ details-value]]
   (assoc-in db [:token-stream-form :erc20-token-name] details-value)))

(re-frame/reg-event-db
 ::fetch-erc20-decimals-success
 (fn [db [_ details-value]]
   (assoc-in db [:token-stream-form :erc20-token-decimals] details-value)))

(re-frame/reg-event-fx
 ::fetch-erc20-details-err
 (fn [{:keys [db]} [_ fallthrough-callback error]]
   {:dispatch (conj fallthrough-callback error)}))

(re-frame/reg-event-fx
 ::ev-call-contract-method
 (fn [{:keys [db]} [_ call-method-args]]
   {::call-method call-method-args}))

(re-frame/reg-event-fx
 ::fetch-erc20-details
 (fn [{:keys [db]} [_ token-addr on-success on-fail]]
   {:db (update-in db [:token-stream-form] dissoc :erc20-token-name :erc20-token-decimals)
    :async-flow {:rules [{:when :seen-all-of?
                          :events [::fetch-erc20-name-success ::fetch-erc20-decimals-success]
                          :dispatch-n [[::async-flow-fx/notify ::fetch-erc20-info-done]
                                       (conj on-success token-addr)]}
                         {:when :seen?
                          :events ::fetch-erc20-details-err
                          :dispatch [::async-flow-fx/notify ::fetch-erc20-info-done]}
                         {:when :seen?
                          :events [[::async-flow-fx/notify ::fetch-erc20-info-done]]
                          :dispatch [::events/decrease-loader-counter]
                          :halt true}]}
    :dispatch-n [[::events/increase-loader-counter]
                 [::ev-call-contract-method [token-addr
                                             erc-20-abi
                                             ["name"]
                                             [::fetch-erc20-name-success]
                                             [::fetch-erc20-details-err on-fail]]]
                 [::ev-call-contract-method [token-addr
                                             erc-20-abi
                                             ["decimals"]
                                             [::fetch-erc20-decimals-success]
                                             [::fetch-erc20-details-err on-fail]]]]}))
