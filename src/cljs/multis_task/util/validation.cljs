(ns multis-task.util.validation)

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
    (when-not (pos? (js/Number. number-str))
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

(defn multiple-of-hour-seconds? [number hours]
  (let [seconds (* hours 60)
        residue (mod seconds number)]
    (when (not= residue 0)
      (str hours " hours (" seconds " in seconds) must be a multiple of " number "."))))

(defn to-multi-field-validation-fn [key]
  (case key
    :date-time-after-now? date-time-after-now?
    :multiple-of-hour-seconds? multiple-of-hour-seconds?))
