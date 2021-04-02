(ns multis-task.events
  (:require
   [re-frame.core :as re-frame]
   [multis-task.db :as db]
   [multis-task.config :as config]
   [multis-task.interceptors :refer [interceptors]]
   [multis-task.util.validation :as validation]))

(re-frame/reg-event-db
 ::initialize-db
 interceptors
 (fn [_ _]
   db/default-db))

(defn change-loader-timer-fx [{:keys [db] :as cofx} op]
  {:db (update db :loader-counter op 1)})
#_(change-loader-timer-fx {:db {:loader-counter 0}} +)

(re-frame/reg-event-fx
 ::increase-loader-counter
 interceptors
 (fn [cofx _]
   (change-loader-timer-fx cofx +)))

(re-frame/reg-event-fx
 ::decrease-loader-counter
 interceptors
 (fn [cofx _]
   (change-loader-timer-fx cofx -)))

(re-frame/reg-event-fx
 :notify-error
 interceptors
 (fn [{:keys [db] :as cofx} [_ error]]
   {:db (update db :ui-errors
                (fn [errors]
                  (conj
                   (remove #(= % error) errors)
                   error)))
    ::clear-error-later error}))

(defn remove-value-from-db [db field-path]
  (update-in db (butlast field-path) dissoc (last field-path)))

(re-frame/reg-event-db
 :on-field-error
 interceptors
 (fn [db [_ field-id field-path error]]
   (-> db
       (assoc-in [:field-errors field-id] error)
       (remove-value-from-db field-path))))

(re-frame/reg-event-db
 :on-field-ok
 interceptors
 (fn [db [_ field-id field-path value]]
   (let [db-err-removed (update db :field-errors dissoc field-id)]
     (if value
       (assoc-in db-err-removed field-path value)
       (remove-value-from-db db-err-removed field-path)))))

(re-frame/reg-event-db
 :clear-error
 interceptors
 (fn [db [_ error]]
   (update db :ui-errors
           (fn [errors]
             (remove #(= % error) errors)))))

(re-frame/reg-fx
 ::clear-error-later
 (fn [error]
   (js/setTimeout
    #(re-frame/dispatch [:clear-error error])
    7001)))

(re-frame/reg-event-db
 ::navigate
 interceptors
 (fn [db [_ to-route]]
   (assoc db :route to-route)))

(re-frame/reg-event-fx
 :println
 (fn [_ [_ & data]]
   (apply println data)))

(re-frame/reg-event-fx
 :console-log
 (fn [_ [_ & data]]
   (apply (.-log js/console) data)))

(re-frame/reg-event-fx
 :clj->js
 (fn [_ [_ notify-callback data]]
   {:dispatch (conj notify-callback (clj->js data))}))

(re-frame/reg-event-fx
 :js->clj
 (fn [_ [_ notify-callback data]]
   {:dispatch (conj notify-callback (js->clj data))}))

(defn to-validate-field-input [{:keys [field-path error-label-id validation-fns]}]
  {:validation-fns validation-fns
   :on-success [:on-field-ok error-label-id field-path]
   :on-fail [:on-field-error error-label-id field-path]
   })

(re-frame/reg-event-fx
 ::validate-field
 interceptors
 (fn [{:keys [db]} [_ {:keys [validation-fns on-success on-fail]} value]]
   {:dispatch (if-let [err (some
                            (fn [fn-key] ((validation/to-validation-fn fn-key) value))
                            validation-fns)]
                (conj on-fail err)
                on-success)}))
