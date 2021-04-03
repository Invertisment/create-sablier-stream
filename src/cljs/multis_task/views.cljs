(ns multis-task.views
  (:require
   [re-frame.core :as re-frame]
   [multis-task.subs :as subs]
   [multis-task.events :as events]
   [multis-task.metamask :as metamask]
   [multis-task.contracts :as contracts]
   [clojure.string :as strs]
   [multis-task.util.form :as form]
   [multis-task.util.form-events :as form-events]))

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

(defn chosen-network-name []
  (if-let [net-name @(re-frame/subscribe [::subs/chosen-network-name])]
    (form/input "Network" [:div net-name])
    [:div]))

(defn chosen-acc-addr []
  (if-let [acc-addr @(re-frame/subscribe [::subs/chosen-acc-addr])]
    (form/input "Account" [:div acc-addr])
    [:div]))

(defn to [to-route btn-name]
  [:button
   {:on-click #(re-frame/dispatch [::events/navigate to-route])}
   btn-name])

(defn token-addr-input [title to-route btn-name]
  [:div
   (form/input "ERC20 Token address"
               [:input
                {:default-value @(re-frame/subscribe [::subs/erc20-token-addr-input])
                 :on-change #(re-frame/dispatch [::contracts/fetch-erc20-name
                                                 (form/->target->value %)
                                                 [::form-events/on-field-ok :token-stream-form [:erc20-token-addr-input] [:erc20-token-input :addr]]
                                                 [::form-events/on-field-error :token-stream-form [:erc20-token-addr-input] [:erc20-token-input]]])}])
   (form/field-error :token-stream-form [:erc20-token-addr-input])
   (form/input "ERC20 Token name"
               (when-let [token-name @(re-frame/subscribe [::subs/erc20-token-name-input])]
                 [:div token-name]))])

(defn token-stream-initiation []
  [:div
   [token-addr-input]
   (when @(re-frame/subscribe [::subs/authorize-sablier-visible])
     [:div
      (form/field-with-err {:label "Desired stream start date"
                            :type "date"
                            :form-id :token-stream-form
                            :field-path [:date-from]
                            :validation-fns [:required :date-at-least-today?]
                            :revalidate-field-on-change [:time-from]})
      (form/field-with-err {:label "Desired stream start time"
                            :type "time"
                            :form-id :token-stream-form
                            :field-path [:time-from]
                            :validation-fns [:required]
                            :multi-validation-field-paths [[:date-from] :_]
                            :multi-validation-fn :date-time-after-now?})
      (form/field-with-err {:label "Duration (hours)"
                            :type "number"
                            :form-id :token-stream-form
                            :field-path [:duration]
                            :validation-fns [:required :pos?]
                            :revalidate-field-on-change [:amount]})
      (form/field-with-err {:label "Amount"
                            :type "number"
                            :form-id :token-stream-form
                            :field-path [:amount]
                            :validation-fns [:required :pos?]
                            :multi-validation-field-paths [[:duration] :_]
                            :multi-validation-fn :hours-multiple-of?})
      [:button
       {:on-click #(re-frame/dispatch [::form-events/revalidate-form
                                       :token-stream-form
                                       [:println "Button on-success"]])}
       "Submit"]
      (let [token-name @(re-frame/subscribe [::subs/erc20-token-name-input])]
        (dispatch-button [:span "Authorize Sablier to use " token-name] [:println "hi"]))])])

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
