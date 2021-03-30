(ns multis-task.db
  (:require [clojure.spec.alpha :as s]))

(def default-db
  {:name "re-frame"
   :loader-counter 0
   })

(s/def ::metamask-data (s/keys :req-un []
                               :opt-un [::network-name ::chosen-account]))

(s/def ::db-spec (s/keys :req-un [::loader-counter ::name]
                         :opt-un [::metamask-data]
                         ))
