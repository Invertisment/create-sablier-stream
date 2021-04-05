(ns multis-task.contracts
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            ["erc-20-abi" :as erc-20-abi]
            [ethers :as ethers]
            [multis-task.util.async :as async]
            [multis-task.events :as events]))

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

(defn- call-read-only-method-fx [[contract-addr abi signer [contract-method contract-args] on-success on-fail]]
  (if (is-addr? contract-addr)
    (let [contract (ethers/Contract. contract-addr abi signer)]
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
    (.getSigner (multis-task.metamask/mk-ethers-provider!))
    ["name"]
    [:println :ok]
    [:println :error]])

(re-frame/reg-fx
 ::call-method
 call-read-only-method-fx)

(re-frame/reg-event-fx
 ::fetch-erc20-name-success
 (fn [{:keys [db]} [_ token-addr success-callback token-name]]
   (let [new-db (assoc-in db [:token-stream-form :erc20-token-name] token-name)]
     (if success-callback
       {:db new-db
        :dispatch success-callback}
       {:db new-db}))))

(re-frame/reg-event-fx
 ::fetch-erc20-name
 (fn [cofx [_ token-addr on-success on-fail]]
   {::call-method [token-addr
                   erc-20-abi
                   (.getSigner (multis-task.metamask/mk-ethers-provider!))
                   ["name"]
                   [::fetch-erc20-name-success token-addr (conj on-success token-addr)]
                   on-fail]}))
