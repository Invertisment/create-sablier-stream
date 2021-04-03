(ns multis-task.util.form-events
  (:require [re-frame.core :as re-frame]
            [multis-task.interceptors :refer [interceptors]]
            [multis-task.util.validation :as validation]))

(defn remove-value-from-db [db field-path]
  (update-in db (butlast field-path) dissoc (last field-path)))

(defn field-path->error-path [field-path]
  (cons :field-errors field-path))
#_(field-path->error-path [:intermediate-path :field-1])

(re-frame/reg-event-db
 ::on-field-error
 interceptors
 (fn [db [_ error-path field-path error]]
   (-> db
       (assoc-in (field-path->error-path error-path) error)
       (remove-value-from-db field-path))))

(re-frame/reg-event-db
 ::on-field-ok
 interceptors
 (fn [db [_ error-path field-path value]]
   (let [db-err-removed (assoc-in db (field-path->error-path error-path) nil)]
     (if value
       (assoc-in db-err-removed field-path value)
       (remove-value-from-db db-err-removed field-path)))))

(re-frame/reg-event-fx
 ::validate-field
 interceptors
 (fn [{:keys [db]} [_ {:keys [validation-fns on-success on-fail]} value]]
   {:dispatch (if-let [err (some
                            (fn [fn-key] ((validation/to-validation-fn fn-key) value))
                            validation-fns)]
                (conj on-fail err)
                on-success)}))

(defn to-validate-field-input [{:keys [field-path error-path validation-fns]}]
  {:validation-fns validation-fns
   :on-success [::on-field-ok error-path field-path]
   :on-fail [::on-field-error error-path field-path]})
