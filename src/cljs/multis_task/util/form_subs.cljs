(ns multis-task.util.form-subs
  (:require [re-frame.core :as re-frame]
            [clojure.string :as strs]
            [multis-task.util.form-utils :as form-utils]))

(def this-ns (namespace ::_))

(defn form-path-to-kw [prefix form-id path]
  (keyword this-ns (str prefix "_" (name form-id) "_" (strs/join "_" (map name path)))))

(defn field-path->error-sub-id [form-id field-path]
  (form-path-to-kw "field-error" form-id field-path))
#_(field-path->error-sub-id :form-id [:form-data :field-1])

(defn field-key->sub-id [form-id field-path]
  (form-path-to-kw "field-value" form-id field-path))
#_(field-key->sub-id :form-id [:field-path-1 :field-path-2])

(re-frame/reg-sub
 ::field-error_token-stream-form_erc20-token-addr
 (fn [db]
   (get-in db [:field-errors :token-stream-form :erc20-token-addr])))

(re-frame/reg-sub
 ::field-error_token-stream-form_date-from
 (fn [db]
   (get-in db [:field-errors :token-stream-form :date-from])))

(re-frame/reg-sub
 ::field-error_token-stream-form_time-from
 (fn [db]
   (get-in db [:field-errors :token-stream-form :time-from])))

(re-frame/reg-sub
 ::field-error_token-stream-form_duration-h
 (fn [db]
   (get-in db [:field-errors :token-stream-form :duration-h])))

(re-frame/reg-sub
 ::field-error_token-stream-form_amount
 (fn [db]
   (get-in db [:field-errors :token-stream-form :amount])))

(re-frame/reg-sub
 ::field-error_token-stream-form_multiple-validation
 (fn [db]
   (get-in db [:field-errors :token-stream-form :multiple-validation])))

(re-frame/reg-sub
 (field-key->sub-id :token-stream-form [:date-from])
 (fn [db]
   (form-utils/db-get db :token-stream-form [:date-from])))

(re-frame/reg-sub
 (field-key->sub-id :token-stream-form [:time-from])
 (fn [db]
   (form-utils/db-get db :token-stream-form [:time-from])))

(re-frame/reg-sub
 (field-key->sub-id :token-stream-form [:duration-h])
 (fn [db]
   (form-utils/db-get db :token-stream-form [:duration-h])))

(re-frame/reg-sub
 (field-key->sub-id :token-stream-form [:amount])
 (fn [db]
   (form-utils/db-get db :token-stream-form [:amount])))
