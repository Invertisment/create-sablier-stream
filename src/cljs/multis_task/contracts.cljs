(ns multis-task.contracts
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            ["erc-20-abi" :as erc-20-abi]
            [ethers :as ethers]
            [multis-task.util.async :as async]
            [multis-task.events :as events]
            [day8.re-frame.async-flow-fx :as async-flow-fx]
            ["bigdecimal" :as bigdecimal]
            [multis-task.util.bigdec :as bigdec]))

(def sablier-contract-addr-rinkeby "0xc04Ad234E01327b24a831e3718DBFcbE245904CC")

(re-frame/reg-event-fx
 :notify-http-failure
 (fn [_ [_ notify-callback data]]
   {:dispatch (conj notify-callback (or (:last-error data)
                                        (:debug-message data)
                                        (:status-text data)
                                        (:status data)))}))

(defn- mk-fetch-abi-event [abi-type on-success on-fail]
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
#_(mk-fetch-abi-event :sablier [:on-success] [:on-fail])
#_(mk-fetch-abi-event :erc20 [:on-success] [:on-fail])
#_(mk-fetch-abi-event :other [:on-success] [:on-fail])

(re-frame/reg-event-fx
 ::fetch-abi
 (fn [_ [_ abi-type on-success on-fail]]
   (mk-fetch-abi-event abi-type on-success on-fail)))

(defn is-addr? [addr]
  (.isAddress (.-utils ethers) addr))

(defn- call-method-fx [contract-addr abi signer [contract-method contract-args] on-success on-fail]
  (if (is-addr? contract-addr)
    (let [contract (ethers/Contract. contract-addr abi (.getSigner (multis-task.metamask/mk-ethers-provider!)))]
      (async/promise->dispatch
       (apply (aget contract contract-method) contract-args)
       {:on-success on-success
        :on-fail on-fail}))
    (re-frame/dispatch (conj on-fail "Bad contract address."))))
#_(call-method-fx
   "0xfab46e002bbf0b4509813474841e0716e6730136"
   #_"0x1111111111111111111111111111111111111111"
   erc-20-abi
   #_(.getSigner (multis-task.metamask/mk-ethers-provider!))
   (multis-task.metamask/mk-ethers-provider!)
   ["name"]
   [:println :ok]
   [:println :error])

(re-frame/reg-fx
 ::call-read-only-method
 (fn [[contract-addr contract-args on-success on-fail abi]]
   (call-method-fx
    contract-addr
    abi
    (multis-task.metamask/mk-ethers-provider!)
    contract-args
    on-success
    on-fail)))

(re-frame/reg-fx
 ::call-read-write-method
 (fn [[contract-addr contract-args on-success on-fail abi]]
   (call-method-fx
    contract-addr
    abi
    (.getSigner (multis-task.metamask/mk-ethers-provider!))
    contract-args
    on-success
    on-fail)))

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
   {::call-read-only-method call-method-args}))

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
                                             ["name"]
                                             [::fetch-erc20-name-success]
                                             [::fetch-erc20-details-err on-fail]
                                             erc-20-abi]]
                 [::ev-call-contract-method [token-addr
                                             ["decimals"]
                                             [::fetch-erc20-decimals-success]
                                             [::fetch-erc20-details-err on-fail]
                                             erc-20-abi]]]}))

(defn calc-deposit [amount erc20-token-decimals]
  (str (.movePointRight (bigdec/str->bigdec amount) (js/Number erc20-token-decimals))))

(defn calc-start-time [date-from time-from]
  (.round js/Math (/ (.getTime (js/Date. (str date-from " " time-from))) 1000)))

(defn calc-end-time [start-time duration-h]
  (+ start-time (* 60 (js/Number duration-h))))

(re-frame/reg-event-fx
 ::erc20-approve-sablier-deposit--success
 (fn [{:keys [db]} [_ to-dispatch]]
   (println "to-dispatch" to-dispatch)
   {}
   {:db (assoc db :erc20-sablier-deposit-approval-done true)
    :dispatch to-dispatch}))

(re-frame/reg-event-fx
 ::erc20-approve-to-sablier
 (fn [{:keys [db]} [_ on-success on-fail {:keys [erc20-token-addr erc20-token-decimals amount date-from time-from duration-h] :as form-fields}]]
   (let [final-deposit (calc-deposit amount erc20-token-decimals)
         start-time (calc-start-time date-from time-from)
         end-time (calc-end-time start-time duration-h)]
     (println (str final-deposit)))
   ;;{:erc20-token-addr 0xfab46e002bbf0b4509813474841e0716e6730136, :date-from 2021-04-08, :time-from 10:04, :duration-h 1, :amount 60}
   {:dispatch [::erc20-approve-sablier-deposit--success on-success]
    ;;::call-read-write-method [erc20-token-addr
    ;;                          ["approve" sablier-contract-addr-rinkeby final-deposit]
    ;;                          [::erc20-approve-sablier-deposit--success]
    ;;                          on-fail
    ;;                          erc-20-abi]
    }))

(re-frame/reg-event-fx
 ::sablier-start-stream--success
 (fn [{:keys [db]} [_ to-dispatch]]
   {:dispatch to-dispatch}))

(re-frame/reg-event-fx
 ::sablier-start-stream
 (fn [{:keys [db]} [_ on-success on-fail {:keys [erc20-token-addr erc20-token-decimals amount date-from time-from duration-h] :as form-fields}]]
   (let [final-deposit (calc-deposit amount erc20-token-decimals)
         start-time (calc-start-time date-from time-from)
         end-time (calc-end-time start-time duration-h)])
   ;;{:erc20-token-addr 0xfab46e002bbf0b4509813474841e0716e6730136, :date-from 2021-04-08, :time-from 10:04, :duration-h 1, :amount 60}
   {:dispatch [::sablier-start-stream--success on-success]
    ;;::call-read-write-method [erc20-token-addr
    ;;                          ["approve" sablier-contract-addr-rinkeby final-deposit]
    ;;                          [::erc20-approve-sablier-deposit--success]
    ;;                          on-fail
    ;;                          erc-20-abi]
    }))

(re-frame/reg-event-fx
 ::reset-stream-flow
 (fn [{:keys [db]} [_ to-dispatch]]
   {:db (assoc db :erc20-sablier-deposit-approval-done false)
    :dispatch to-dispatch}))
