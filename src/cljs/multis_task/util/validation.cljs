(ns multis-task.util.validation
  (:require [multis-task.util.bigdec :as bigdec]
            ["ethers" :as ethers]))

(defn- get-day-date [date]
  (js/Date. (subs (.toISOString date) 0 10)))

(defn- get-current-day-date! []
  (get-day-date (js/Date.)))

(defn date-at-least-today? [date-str]
  (when-not (empty? date-str)
    (let [today-date (get-current-day-date!)
          input-date (get-day-date (js/Date. date-str))]
      (when (> today-date input-date)
        "Expected to be least today's date."))))

(defn required [data]
  (when (empty? data)
    "This field is required."))

(defn pos-str? [number-str]
  (try
    (when-not (= 1 (.signum (bigdec/->bigdec number-str)))
      "The number should be positive.")
    (catch js/Object e
      "Expected a number input.")))

(defn address? [maybe-addr-str]
  (when-not (.isAddress ethers/utils maybe-addr-str)
    "Not a valid address."))

(defn to-validation-fn [key]
  (case key
    :date-at-least-today? date-at-least-today?
    :pos? pos-str?
    :required required
    :address? address?
    ))

(defn date-time-after-now? [date-str time-str]
  (when (and date-str time-str)
    (let [input (js/Date. (str date-str " " time-str))
          now (js/Date.)]
      (when (> now input)
        "Expected future Date and Time."))))

(defn to-seconds [duration-h]
  (.multiply
   (bigdec/->bigdec duration-h)
   (bigdec/->bigdec (* 60 60))))
#_(str (to-seconds "2"))

(defn amount-multiple-of-hour-seconds? [amount decimals hours]
  (try
    (let [amount (bigdec/->bigdec amount decimals)
          ten-pow-to-decimals (.pow (bigdec/->bigdec "10") decimals)
          seconds (to-seconds hours)
          seconds-div (.stripTrailingZeros (.movePointLeft seconds decimals))
          remainder (.remainder amount seconds-div)
          closest-match-bottom (.stripTrailingZeros (.subtract amount remainder))
          closest-match-top (.stripTrailingZeros (.add closest-match-bottom seconds-div))]
      (when-not (bigdec/bigdec-zero? remainder)
        [:div
         (str "Amount " (.stripTrailingZeros amount) " must be a multiple of " seconds-div " (" hours "h*60*60*10^-"decimals").")
         (when-not (or (bigdec/bigdec-zero? amount)
                       (bigdec/bigdec-zero? seconds))
           [:div
            (if (bigdec/bigdec-zero? closest-match-bottom)
              (str "Closest matching amount is " closest-match-top ".")
              (str "Closest matching amounts are " closest-match-bottom " and " closest-match-top "."))])]))
    (catch js/Object e
      (.toString e))))

(defn to-multi-field-validation-fn [key]
  (case key
    :date-time-after-now? date-time-after-now?
    :amount-multiple-of-hour-seconds? amount-multiple-of-hour-seconds?))
