(ns multis-task.util.form
  (:require [clojure.string :as strs]
            [re-frame.core :as re-frame]
            [multis-task.util.form-subs :as form-subs]
            [multis-task.util.form-events :as form-events]))

(def err-subs-ns (namespace ::form-subs/_))
(defn field-path->error-sub-id [field-path]
  (keyword err-subs-ns (str "field-error_" (strs/join "_" (map name field-path)))))
#_(field-path->error-sub-id [:form-data :field-1])

(defn ->target->value [event]
  (-> event
      (aget "target")
      (aget "value")))

(defn mk-on-change-dispatch-fn [validate-field-input]
  #(re-frame/dispatch
    [::form-events/validate-field
     (form-events/to-validate-field-input
      validate-field-input)
     (->target->value %)]))

(defn input [label field]
  [:div.input
   [:div label]
   field])

(defn field-error [field-path]
  (when-let [err @(re-frame/subscribe [(field-path->error-sub-id field-path)])]
    [:div.field-error err]))

(defn field-with-err [{:keys [label type field-path error-path validation-fns] :as field-description}]
  (let [mandatory-error-path (or error-path field-path)]
    [:div
     (input
      label
      [:input
       {:type type
        :on-change (mk-on-change-dispatch-fn
                    (merge
                     {:error-path mandatory-error-path}
                     (select-keys
                      field-description
                      [:field-path :validation-fns])))}])
     (field-error mandatory-error-path)]))
