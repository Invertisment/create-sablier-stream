(ns multis-task.views
  (:require
   [re-frame.core :as re-frame]
   [multis-task.subs :as subs]
   [multis-task.events :as events]
   [multis-task.metamask :as metamask]
   [multis-task.contracts :as contracts]))

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

(defn token-addr-input [title to-route btn-name]
  [:div
   (input "ERC20 Token address"
          [:input
           {:default-value @(re-frame/subscribe [::subs/erc20-token-addr-input])
            :on-change #(re-frame/dispatch [::contracts/fetch-erc20-name
                                            (-> %
                                                (aget "target")
                                                (aget "value"))])}])
   (input "ERC20 Token name"
          (when-let [token-name @(re-frame/subscribe [::subs/erc20-token-name-input])]
            [:div token-name]))])

(defn authorize-sablier []
  (if @(re-frame/subscribe [::subs/authorize-sablier-visible])
    [:div
     (input "ERC20 amount" [:input])
     (let [token-name @(re-frame/subscribe [::subs/erc20-token-name-input])]
       (dispatch-button [:span "Authorize Sablier to use " token-name] [:println "hi"]))]
    [:div]))

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

(defn metamask-page [title & body]
  (apply page title [metamask-info] body))
#_(metamask-page "hi" :1 :2 :3)

(defn main-panel []
  (let [route @(re-frame/subscribe [::subs/route])]
    (case route
      :home (page
             "ERC20 streaming"
             (dispatch-button "Connect Metamask" [::metamask/activate-metamask]))
      :connected-menu (metamask-page
                       "Choose your action"
                       (to :initiate-token-stream "Create token stream"))
      :initiate-token-stream (metamask-page
                              "Create token stream"
                              [token-addr-input]
                              [authorize-sablier]
                              (dispatch-button "Fetch Sablier ABI" [::contracts/fetch-abi :sablier [:console-log] [:notify-error]])
                              (dispatch-button "Fetch erc20 ABI" [::contracts/fetch-abi :erc20 [:console-log] [:notify-error]])
                              (dispatch-button "Fetch bruh ABI" [::contracts/fetch-abi :bruh [:console-log] [:notify-error]])
                              (dispatch-button "Call FAU contract" [::contracts/call-FAU-name])
                              (to :home "Go to Home"))
      [:div
       [:h1 "Not found"]
       [to :home "Return to start"]])))
