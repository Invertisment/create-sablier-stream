(ns multis-task.util.form
  (:require [re-frame.core :as re-frame]
            [multis-task.util.form-subs :as form-subs]
            [multis-task.util.form-events :as form-events]))

(defn ->target->value [event]
  (-> event
      (aget "target")
      (aget "value")))

(defn mk-on-change-dispatch-fn! [{:keys [form-id field-path] :as on-change-dispatch-input}]
  (when (and (:revalidate-field-on-change on-change-dispatch-input)
             (:multi-validation-fn on-change-dispatch-input))
    (throw (ex-info "Invalid field specification.
Existence of :revalidate-field-on-change and :multi-validation-fn on a single field will result in unending event cycle."
                    (clj->js on-change-dispatch-input))))
  (let [validate-field-input (form-events/to-validate-field-input
                              on-change-dispatch-input)]
    (re-frame/dispatch (form-events/mk-put-field-invalidation-event
                        form-id
                        field-path
                        validate-field-input))
    #(re-frame/dispatch
      [::form-events/validate-field
       validate-field-input
       (->target->value %)])))

(defn input [label field]
  [:div.input
   [:div label]
   field])

(defn field-error [form-id field-path]
  (when-let [err @(re-frame/subscribe [(form-subs/field-path->error-sub-id form-id field-path)])]
    [:div.field-error err]))

(defn field-with-err [{:keys [form-id label type field-path error-path validation-fns input-props] :as field-description}]
  (let [mandatory-error-path (or error-path field-path)]
    [:div
     (input
      label
      [:input
       (merge
        input-props
        {:type type
         :on-change (mk-on-change-dispatch-fn!
                     (merge
                      {:error-path mandatory-error-path}
                      (select-keys
                       field-description
                       [:form-id :field-path :validation-fns
                        :multi-validation-field-paths
                        :multi-validation-fn
                        :revalidate-field-on-change])))
         :default-value @(re-frame/subscribe
                          [(form-subs/field-key->sub-id
                            form-id
                            field-path)])})])
     (field-error form-id mandatory-error-path)]))
