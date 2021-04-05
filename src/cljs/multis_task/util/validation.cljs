(ns multis-task.util.validation
  (:require ["bigdecimal" :as bigdecimal]))

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
    (when-not (= 1 (.signum (bigdecimal/BigDecimal. number-str)))
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

(defn str->bigdec
  ([s] (bigdecimal/BigDecimal. s))
  ([s scale] (.setScale
              (bigdecimal/BigDecimal. s)
              (js/Number scale))))
#_(.toString (str->bigdec 5 2))
#_(.toString (str->bigdec "5"))
#_(.toString (str->bigdec 5))

(def bigdec-rounding-mode (.HALF_UP bigdecimal/RoundingMode))

(def bigdec-zero (str->bigdec "0"))
#_(.toString bigdec-zero)

(defn bigdec-zero? [bigdec]
  (= 0 (.compareTo bigdec-zero bigdec)))
#_(bigdec-zero? (str->bigdec 0 4))

(defn amount-multiple-of-hour-seconds? [amount decimals hours]
  (try
    (let [amount (str->bigdec amount decimals)
          ten-pow-to-decimals (.pow (str->bigdec "10") decimals)
          seconds (.multiply (str->bigdec hours) (str->bigdec "60"))
          seconds-div (.divide (.multiply (str->bigdec hours) (str->bigdec "60"))
                               ten-pow-to-decimals
                               decimals
                               bigdec-rounding-mode)
          remainder (.remainder amount seconds-div)
          closest-match-bottom (.subtract amount remainder)
          closest-match-top (.add closest-match-bottom seconds-div)]
      (when-not (bigdec-zero? remainder)
        [:div
         (str "Amount (" amount ") must be a multiple of " seconds-div " (" hours "h * 60 / (10^"decimals")).")
         (when-not (or (bigdec-zero? amount)
                       (bigdec-zero? seconds))
           [:div
            (if (bigdec-zero? closest-match-bottom)
              (str "Closest matching amount is " closest-match-top ".")
              (str "Closest matching amounts are " closest-match-bottom " and " closest-match-top "."))])]))
    (catch js/Object e
      (.toString e))))

(defn to-multi-field-validation-fn [key]
  (case key
    :date-time-after-now? date-time-after-now?
    :amount-multiple-of-hour-seconds? amount-multiple-of-hour-seconds?))
