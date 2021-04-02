(ns multis-task.util.validation)

(defn- get-day-date [date]
  (js/Date. (subs (.toISOString date) 0 10)))

(defn- get-current-day-date! []
  (get-day-date (js/Date.))
  #_(js/Date. (subs (.toISOString (js/Date.)) 0 10)))

(defn date-at-least-today? [date-str]
  (when-not (empty? date-str)
    (let [today-date (get-current-day-date!)
          input-date (get-day-date (js/Date. date-str))]
      (when (> today-date input-date)
        "Expected to be least today's date."))))

(defn required [data]
  (when (empty? data)
    "This field is required"))

(defn pos-str? [number-str]
  (try
    (when-not (pos? (js/Number. number-str))
      "The number should be positive.")
    (catch js/Object e
      (println e)
      "Expected a number input.")))

(defn to-validation-fn [key]
  (case key
    :date-at-least-today? date-at-least-today?
    :pos? pos-str?
    :required required
    ))
