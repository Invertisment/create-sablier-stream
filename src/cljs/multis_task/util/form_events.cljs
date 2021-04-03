(ns multis-task.util.form-events
  (:require [re-frame.core :as re-frame]
            [multis-task.interceptors :refer [interceptors]]
            [multis-task.util.validation :as validation]
            [day8.re-frame.async-flow-fx :as async-flow-fx]))

(defn full-db-path [form-id path]
  (cons form-id path))

(defn remove-value-from-db [db form-id field-path]
  (update-in db (full-db-path form-id (butlast field-path)) dissoc (last field-path)))

(defn to-db-error-path [form-id field-path]
  (cons :field-errors (full-db-path form-id field-path)))
#_(to-db-error-path [:intermediate-path :field-1])

(re-frame/reg-event-db
 ::on-field-error
 interceptors
 (fn [db [_ form-id error-path field-path error]]
   (-> db
       (assoc-in (to-db-error-path form-id error-path) error)
       (remove-value-from-db form-id field-path))))

(re-frame/reg-event-db
 ::on-field-ok
 interceptors
 (fn [db [_ form-id error-path field-path value]]
   (let [db-err-removed (assoc-in db (to-db-error-path form-id error-path) nil)]
     (if value
       (assoc-in db-err-removed (full-db-path form-id field-path) value)
       (remove-value-from-db db-err-removed form-id field-path)))))

(defn to-validate-field-input [{:keys [form-id field-path error-path validation-fns multi-validation-field-paths multi-validation-fn]}]
  {:form-id form-id
   :validation-fns validation-fns
   :on-success [::on-field-ok form-id error-path field-path]
   :on-fail [::on-field-error form-id error-path field-path]
   :multi-validation-field-paths multi-validation-field-paths
   :multi-validation-fn multi-validation-fn
   })

(defn validate-field-value [validation-fns value]
  (some
   (fn [fn-key] ((validation/to-validation-fn fn-key) value))
   validation-fns))

(defn db-get [db form-id path]
  (get-in db (full-db-path form-id path)))

(defn validate-multiple-field-values [db form-id multi-validation-fn multi-validation-field-paths value]
  (when (and multi-validation-field-paths multi-validation-fn)
    (let [args (map
                (fn [token-or-field-path]
                  (case token-or-field-path
                    :_ value
                    (db-get db form-id token-or-field-path)))
                multi-validation-field-paths)]
      (apply (validation/to-multi-field-validation-fn multi-validation-fn) args))))

(re-frame/reg-event-fx
 ::validate-field
 interceptors
 (fn [{:keys [db]} [_ {:keys [form-id validation-fns on-success on-fail multi-validation-field-paths multi-validation-fn]} value]]
   {:dispatch (if-let [err (or (validate-field-value validation-fns value)
                               (validate-multiple-field-values db form-id multi-validation-fn multi-validation-field-paths value))]
                (conj on-fail err)
                (conj on-success value))}))

(defn mk-put-field-invalidation-event [form-id field-path validate-field-input]
  [::put-field-invalidation-event
   {:validate-field-input-no-value [::validate-field validate-field-input]
    :form-id form-id
    :field-path field-path}])

(defn mk-form-invalidation-path [form-id field-path]
  (cons :form-invalidation (full-db-path form-id field-path)))

;; HACK: This is a small memory leak.
;; If there will be a lot of distinct forms (millions?) then it will be noticeable.
(re-frame/reg-event-db
 ::put-field-invalidation-event
 interceptors
 (fn [db [_ {:keys [form-id field-path validate-field-input-no-value] :as input}]]
   (assoc-in
    db
    (mk-form-invalidation-path form-id field-path)
    (select-keys input [:field-path :validate-field-input-no-value]))))

(defn mk-field-status-event-matcher [original-event]
  (fn [event-with-args]
    (= (take 4 original-event)
       (take 4 event-with-args))))

(defn form-event-callback-by-type [event-type form-invalidation-event]
  (->> form-invalidation-event
       second
       event-type))

(defn mk-event-matchers [event-type form-invalidation-events]
  (map (comp
        mk-field-status-event-matcher
        (partial form-event-callback-by-type event-type))
       form-invalidation-events))

(defn get-form-invalidation-events [form-id db]
  (->> (mk-form-invalidation-path form-id [])
       (get-in db)
       (vals)
       (map (fn [{:keys [validate-field-input-no-value field-path]}]
              (conj validate-field-input-no-value (db-get db form-id field-path))))))

(re-frame/reg-event-fx
 ::revalidate-form
 interceptors
 (fn [{:keys [db]} [_ form-id on-success]]
   (let [form-invalidation-events (get-form-invalidation-events form-id db)
         success-event-matchers (mk-event-matchers :on-success form-invalidation-events)
         fail-event-matchers (mk-event-matchers :on-fail form-invalidation-events)]
     ;; Possible optimization: Create an event that handles multiple input validations at once (reduce events).
     {:dispatch-n form-invalidation-events
      :async-flow {:rules [{:when :seen-all-of? :events success-event-matchers
                            :dispatch-n (remove nil? [[::async-flow-fx/notify :end] on-success])}
                           {:when :seen-any-of? :events fail-event-matchers
                            :dispatch-n [[::async-flow-fx/notify :end]]}
                           {:when :seen?
                            :events [[::async-flow-fx/notify :end]]
                            ;;:dispatch [:println "Flow ended"]
                            :halt? true}]}})))
