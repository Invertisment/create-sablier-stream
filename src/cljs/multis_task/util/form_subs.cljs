(ns multis-task.util.form-subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::field-error_token-stream-form_erc20-token-addr-input
 (fn [db]
   (get-in db [:field-errors :token-stream-form :erc20-token-addr-input])))

(re-frame/reg-sub
 ::field-error_token-stream-form_date-from
 (fn [db]
   (get-in db [:field-errors :token-stream-form :date-from])))

(re-frame/reg-sub
 ::field-error_token-stream-form_time-from
 (fn [db]
   (get-in db [:field-errors :token-stream-form :time-from])))

(re-frame/reg-sub
 ::field-error_token-stream-form_duration
 (fn [db]
   (get-in db [:field-errors :token-stream-form :duration])))

(re-frame/reg-sub
 ::field-error_token-stream-form_amount
 (fn [db]
   (get-in db [:field-errors :token-stream-form :amount])))

(re-frame/reg-sub
 ::field-error_token-stream-form_multiple-validation
 (fn [db]
   (get-in db [:field-errors :token-stream-form :multiple-validation])))
