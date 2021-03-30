(ns multis-task.views
  (:require
   [re-frame.core :as re-frame]
   [multis-task.subs :as subs]
   [multis-task.events :as events]))

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

(defn link-btn [btn-name to-route]
  [:button
   {:on-click #(re-frame/dispatch [::events/navigate to-route])}
   btn-name])

(defn metamask-info []
  [:div
   [chosen-network-name]
   [chosen-acc-addr]])

(defn page [title & body]
  [:div
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
             (dispatch-button "Connect Metamask" [::events/activate-metamask]))
      :connected-menu (metamask-page
                       "Choose your action"
                       (link-btn "Create token stream" :initiate-token-stream))
      :initiate-token-stream (metamask-page
                              "Create token stream"
                              (link-btn "Go to Home" :home))
      [:div
       [:h1 "Not found"]
       [link-btn "Return to start" :home]])))
