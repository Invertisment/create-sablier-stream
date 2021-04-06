(ns multis-task.util.validation
  (:require [multis-task.util.bigdec :as bigdec]))

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
    (when-not (= 1 (.signum (bigdec/str->bigdec number-str)))
      "The number should be positive.")
    (catch js/Object e
      "Expected a number input.")))

(defn to-validation-fn [key]
  (case key
    :date-at-least-today? date-at-least-today?
    :pos? pos-str?
    :required required
    ))

(defn date-time-after-now? [date-str time-str]
  (when (and date-str time-str)
    (let [input (js/Date. (str date-str " " time-str))
          now (js/Date.)]
      (when (> now input)
        "Expected future Date and Time."))))

(defn amount-multiple-of-hour-seconds? [amount decimals hours]
  (try
    (let [amount (bigdec/str->bigdec amount decimals)
          ten-pow-to-decimals (.pow (bigdec/str->bigdec "10") decimals)
          seconds (-> (bigdec/str->bigdec hours)
                      (.multiply (bigdec/str->bigdec "60"))
                      (.multiply (bigdec/str->bigdec "60")))
          seconds-div (.movePointLeft seconds decimals)
          remainder (.remainder amount seconds-div)
          closest-match-bottom (.subtract amount remainder)
          closest-match-top (.add closest-match-bottom seconds-div)]
      (when-not (bigdec/bigdec-zero? remainder)
        [:div
         (str "Amount (" amount ") must be a multiple of " seconds-div " (" hours "h*60*60*10^-"decimals").")
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
