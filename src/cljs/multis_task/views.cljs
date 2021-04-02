(ns multis-task.views
  (:require
   [re-frame.core :as re-frame]
   [multis-task.subs :as subs]
   [multis-task.events :as events]
   [multis-task.metamask :as metamask]
   [multis-task.contracts :as contracts]
   [clojure.walk :as walk]))

(defn loader []
  (let [loading @(re-frame/subscribe [::subs/loading])]
    (if loading
      [:div.portal-container
       [:div.portal
        [:h3 "Loading"]]]
      [:div])))

(defn dispatch-button [title dispatch-msg]
  [:div
   [:button
    {:on-click #(re-frame/dispatch dispatch-msg)}
    title]])

(defn input [label field]
  [:div.input
   [:div label]
   field])

(def subs-ns (namespace ::subs/_))
(defn field-id->error-sub-id [field-id]
  (keyword subs-ns (str "field-error_" (name field-id))))

(defn field-error [field-id]
  (when-let [err @(re-frame/subscribe [(field-id->error-sub-id field-id)])]
    [:div.field-error err]))

(defn chosen-network-name []
  (if-let [net-name @(re-frame/subscribe [::subs/chosen-network-name])]
    (input "Network" [:div net-name])
    [:div]))

(defn chosen-acc-addr []
  (if-let [acc-addr @(re-frame/subscribe [::subs/chosen-acc-addr])]
    (input "Account" [:div acc-addr])
    [:div]))

(defn to [to-route btn-name]
  [:button
   {:on-click #(re-frame/dispatch [::events/navigate to-route])}
   btn-name])

(defn ->target->value [event]
  (-> event
      (aget "target")
      (aget "value")))

(defn token-addr-input [title to-route btn-name]
  [:div
   (input "ERC20 Token address"
          [:input
           {:default-value @(re-frame/subscribe [::subs/erc20-token-addr-input])
            :on-change #(re-frame/dispatch [::contracts/fetch-erc20-name
                                            (->target->value %)
                                            [:on-field-ok :erc20-token-addr-input [:token-stream-form :erc20-token-input :addr]]
                                            [:on-field-error :erc20-token-addr-input [:token-stream-form :erc20-token-input]]])}])
   (field-error :erc20-token-addr-input)
   (input "ERC20 Token name"
          (when-let [token-name @(re-frame/subscribe [::subs/erc20-token-name-input])]
            [:div token-name]))])

(defn token-stream-initiation []
  [:div
   [token-addr-input]
   (when @(re-frame/subscribe [::subs/authorize-sablier-visible])
     [:div
      (input "Desired stream start date" [:input
                                          {:type "date"
                                           :on-change #(re-frame/dispatch
                                                        [::events/validate-field
                                                         (events/to-validate-field-input
                                                          {:field-path [:token-stream-form :date-from]
                                                           :error-label-id :date-from-input
                                                           :validation-fns [:required :date-at-least-today?]})
                                                         (->target->value %)])}])
      (field-error :date-from-input)
      (input "Desired stream start time" [:input
                                          {:type "time"
                                           :on-change #(re-frame/dispatch
                                                        [::events/validate-field
                                                         (events/to-validate-field-input
                                                          {:field-path [:token-stream-form :time-from]
                                                           :error-label-id :time-from-input
                                                           :validation-fns [:required]})
                                                         (->target->value %)])}])
      (field-error :time-from-input)
      (input "Duration (hours)" [:input {:type "number"
                                         :on-change #(re-frame/dispatch
                                                      [::events/validate-field
                                                       (events/to-validate-field-input
                                                        {:field-path [:token-stream-form :duration]
                                                         :error-label-id :duration-input
                                                         :validation-fns [:required :pos?]})
                                                       (->target->value %)])}])
      (field-error :duration-input)
      (input "Amount" [:input {:type "number"
                               :on-change #(re-frame/dispatch
                                            [::events/validate-field
                                             (events/to-validate-field-input
                                              {:field-path [:token-stream-form :amount]
                                               :error-label-id :amount-input
                                               :validation-fns [:required :pos?]})
                                             (->target->value %)])}])
      (field-error :amount-input)
      (let [token-name @(re-frame/subscribe [::subs/erc20-token-name-input])]
        (dispatch-button [:span "Authorize Sablier to use " token-name] [:println "hi"]))])
   ])

(defn ui-errors [to-route btn-name]
  (when-let [errors (seq @(re-frame/subscribe [::subs/ui-errors]))]
    [:div
     (map
      (fn [error]
        ^{:key error} [:div.space-between.error error (dispatch-button "Clear" [:clear-error error])])
      errors)]))

(defn metamask-info []
  [:div
   [chosen-network-name]
   [chosen-acc-addr]])

(defn page [title & body]
  [:div
   [ui-errors]
   [:h1 title]
   (vec (cons :div body))])
#_(page "hi" :1 :2 :3)

(defn main-panel []
  (let [route @(re-frame/subscribe [::subs/route])]
    (case route
      :home (page
             "ERC20 streaming"
             (dispatch-button "Connect Metamask" [::metamask/activate-metamask [::events/navigate :initiate-token-stream]]))
      :initiate-token-stream (page
                              "Create token stream"
                              [metamask-info]
                              [token-stream-initiation]
                              (dispatch-button "Fetch Sablier ABI" [::contracts/fetch-abi :sablier [:console-log] [:notify-error]])
                              (dispatch-button "Fetch erc20 ABI" [::contracts/fetch-abi :erc20 [:console-log] [:notify-error]])
                              (dispatch-button "Fetch bruh ABI" [::contracts/fetch-abi :bruh [:console-log] [:notify-error]])
                              (dispatch-button "Call FAU contract" [::contracts/call-FAU-name])
                              (to :home "Go to Home")
                              [:div "FAU addr: 0xfab46e002bbf0b4509813474841e0716e6730136"])
      [:div
       [:h1 "Not found"]
       [to :home "Return to start"]])))
