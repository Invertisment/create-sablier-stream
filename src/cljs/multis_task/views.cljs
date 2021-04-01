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
  [:button
   {:on-click #(re-frame/dispatch dispatch-msg)}
   title])

(defn chosen-network-name []
  (if-let [net-name @(re-frame/subscribe [::subs/chosen-network-name])]
    [:div
     [:p "Network: " net-name]]
    [:div]))

(defn chosen-acc-addr []
  (if-let [acc-addr @(re-frame/subscribe [::subs/chosen-acc-addr])]
    [:div
     [:p "Account: " acc-addr]]
    [:div]))

(defn to [to-route btn-name]
  [:button
   {:on-click #(re-frame/dispatch [::events/navigate to-route])}
   btn-name])

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
                              (dispatch-button "Fetch Sablier ABI" [::contracts/fetch-abi :sablier [:console-log] [:notify-error]])
                              (dispatch-button "Fetch erc20 ABI" [::contracts/fetch-abi :erc20 [:console-log] [:notify-error]])
                              (dispatch-button "Fetch bruh ABI" [::contracts/fetch-abi :bruh [:console-log] [:notify-error]])
                              (to :home "Go to Home"))
      [:div
       [:h1 "Not found"]
       [to :home "Return to start"]])))
