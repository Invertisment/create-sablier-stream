(ns multis-task.views
  (:require
   [re-frame.core :as re-frame]
   [multis-task.subs :as subs]
   [multis-task.events :as events]))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]]))

(defn loader []
  (let [loading @(re-frame/subscribe [::subs/loading])]
    (if loading
      [:div
       [:h3 "Loading: " (str loading)]]
      [:div])))

(defn loader-counter-inc-button []
  [:button
   {:on-click #(re-frame/dispatch [::events/change-loader-counter +])}
   "inc"])

(defn loader-counter-dec-button []
  [:button
   {:on-click #(re-frame/dispatch [::events/change-loader-counter -])}
   "dec"])

(defn request-accs-button []
  [:button
   {:on-click #(re-frame/dispatch [::events/activate-metamask])}
   "request accounts"])
