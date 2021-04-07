(ns multis-task.views
  (:require
   [re-frame.core :as re-frame]
   [multis-task.subs :as subs]
   [multis-task.events :as events]
   [multis-task.metamask :as metamask]
   [multis-task.contracts :as contracts]
   [clojure.string :as strs]
   [multis-task.util.form :as form]
   [multis-task.util.form-events :as form-events]
   [multis-task.util.form-subs :as form-subs]))

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
                {:default-value @(re-frame/subscribe [::subs/erc20-token-addr])
                 :on-change #(re-frame/dispatch [::contracts/fetch-erc20-details
                                                 (form/->target->value %)
                                                 [::form-events/on-field-ok :token-stream-form [:erc20-token-addr] [:erc20-token-addr] (form/->target->value %)]
                                                 [::form-events/on-field-error :token-stream-form [:erc20-token-addr] [:erc20-token-addr] (form/->target->value %)]])}])
   (form/field-error :token-stream-form [:erc20-token-addr])
   (form/input "ERC20 Token name"
               (when-let [token-name @(re-frame/subscribe [::subs/erc20-token-name])]
                 [:div token-name]))
   (form/input "ERC20 Token decimals"
               (when-let [token-name @(re-frame/subscribe [::subs/erc20-token-decimals])]
                 [:div token-name]))])

(defn token-stream-initiation []
  [:div
   [token-addr-input]
   (let [authorize-sablier-disabled @(re-frame/subscribe [::subs/authorize-sablier-disabled])]
     (when @(re-frame/subscribe [::subs/authorize-sablier-visible])
       [:div
        (form/field-with-err {:label "Stream start date"
                              :type "date"
                              :form-id :token-stream-form
                              :field-path [:date-from]
                              :validation-fns [:required :date-at-least-today?]
                              :revalidate-field-on-change [:time-from]
                              :input-props {:disabled authorize-sablier-disabled}})
        (form/field-with-err {:label "Stream start time"
                              :type "time"
                              :form-id :token-stream-form
                              :field-path [:time-from]
                              :validation-fns [:required]
                              :multi-validation-field-paths [[:date-from] :_]
                              :multi-validation-fn :date-time-after-now?
                              :input-props {:disabled authorize-sablier-disabled}})
        (form/field-with-err {:label "Stream duration (hours)"
                              :type "text"
                              :form-id :token-stream-form
                              :field-path [:duration-h]
                              :validation-fns [:required :pos?]
                              :revalidate-field-on-change [:amount]
                              :input-props {:disabled authorize-sablier-disabled}})
        (form/field-with-err {:label "ERC20 Token amount"
                              :type "text"
                              :form-id :token-stream-form
                              :field-path [:amount]
                              :validation-fns [:required :pos?]
                              :multi-validation-field-paths [:_ [:erc20-token-decimals] [:duration-h]]
                              :multi-validation-fn :amount-multiple-of-hour-seconds?
                              :input-props {:disabled authorize-sablier-disabled}})
        (form/field-with-err {:label "Final recipient"
                              :type "text"
                              :form-id :token-stream-form
                              :field-path [:recipient-addr]
                              :validation-fns [:required :address?]
                              :input-props {:disabled authorize-sablier-disabled}})
        [:button
         {:on-click #(re-frame/dispatch [::form-events/revalidate-form
                                         :token-stream-form
                                         [::contracts/erc20-approve-to-sablier
                                          [:notify-success "Approved. To activate the stream click on Stream activation."]
                                          [:notify-error]]])
          :disabled authorize-sablier-disabled}
         (let [token-name @(re-frame/subscribe [::subs/erc20-token-name])]
           [:span "Approve Sablier to use " token-name])]
        (when @(re-frame/subscribe [::subs/sablier-stream-activation-visible])
          [:div
           [:button
            {:on-click #(re-frame/dispatch [::form-events/with-form-values
                                            :token-stream-form
                                            [::contracts/sablier-start-stream
                                             [::events/navigate :stream-creation-success]
                                             [:notify-error]]])}
            [:span "Activate Sablier token stream"]]])]))])

(defn preview-notifs [notif-type element-with-classes notifs]
  (map
   (fn [notif]
     ^{:key notif} [element-with-classes notif (dispatch-button "Clear" [:clear-notif notif-type notif])])
   notifs))

(defn ui-notifs []
  [:div
   (concat
    (preview-notifs
     :ui-errors :div.space-between.error @(re-frame/subscribe [::subs/ui-errors]))
    (preview-notifs
     :ui-successes :div.space-between.success @(re-frame/subscribe [::subs/ui-successes])))])

(defn metamask-info []
  [:div
   [chosen-network-name]
   [chosen-acc-addr]])

(defn debug-helpers []
  [:div
   (dispatch-button "Print ERC20 ABI" [::contracts/fetch-abi :erc20 [:console-log] [:println]])
   [:div "FAU addr: 0xfab46e002bbf0b4509813474841e0716e6730136"]])

(defn page [title & body]
  [:div
   [ui-notifs]
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
                              #_[debug-helpers])
      :stream-creation-success (page
                                "Stream created"
                                [metamask-info]
                                [:p.success "Stream created. Head to Etherscan and enter your account address."]
                                (dispatch-button "Create a new payment stream" [::contracts/reset-stream-flow [::events/navigate :initiate-token-stream]]))
      [:div
       [:h1 "Not found"]
       [to :home "Return to start"]])))
