(ns multis-task.db
  (:require [clojure.spec.alpha :as s]))

(def default-db
  {:name "re-frame"
   :loader-counter 0
   })

(s/def ::db-spec (s/keys :req-un [::loader-counter ::name]
                         :opt-un []
                         ))
