(ns multis-task.contracts
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            ["erc-20-abi" :as erc-20-abi]
            [ethers :as ethers]
            [multis-task.util.async :as async]))

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
                           :on-success [:clj->js on-success]
                           :on-failure [:notify-http-failure on-fail]}}
    :erc20 {:dispatch (conj on-success erc-20-abi)}
    {:dispatch (conj on-fail "Unknown token")}))
#_(mk-fetch-abi-event :cofx [:e :sablier [:on-success] [:on-fail]])
#_(mk-fetch-abi-event :cofx [:e :erc20 [:on-success] [:on-fail]])
#_(mk-fetch-abi-event :cofx [:e :other [:on-success] [:on-fail]])

(re-frame/reg-event-fx
 ::fetch-abi
 mk-fetch-abi-event)

(defn- call-read-only-method [_ [_ contract-addr abi signer [contract-method contract-args] on-success on-fail]]
  (let [contract (ethers/Contract. contract-addr abi signer)]
    {}))
#_(call-read-only-method
   :dummy-cofx
   [:dummy-event-name
    "0xfab46e002bbf0b4509813474841e0716e6730136"
    erc-20-abi
    (.getSigner (multis-task.metamask/mk-ethers-provider!))
    ["name"]
    [:on-success]
    [:on-fail]])

(re-frame/reg-fx
 ::call-method
 call-read-only-method)
