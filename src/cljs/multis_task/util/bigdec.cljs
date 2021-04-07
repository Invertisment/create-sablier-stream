(ns multis-task.util.bigdec
  (:require ["bigdecimal" :as bigdecimal]))

(defn ->bigdec
  ([s] (bigdecimal/BigDecimal. s))
  ([s scale] (.setScale
              (bigdecimal/BigDecimal. s)
              (js/Number scale))))
#_(.toString (->bigdec 5 2))
#_(.toString (->bigdec "5"))
#_(.toString (->bigdec 5))

(def bigdec-zero (->bigdec "0"))
#_(.toString bigdec-zero)

(defn bigdec-zero? [bigdec]
  (= 0 (.compareTo bigdec-zero bigdec)))
#_(bigdec-zero? (->bigdec 0 4))

(def round-mode-half-up (.HALF_UP bigdecimal/RoundingMode))
