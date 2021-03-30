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

(defn request-accs-button []
  [:button
   {:on-click #(re-frame/dispatch [::events/activate-metamask])}
   "request accounts"])

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

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]
     [request-accs-button]
     [chosen-network-name]
     [chosen-acc-addr]]))
