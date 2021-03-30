(ns multis-task.db
  (:require [clojure.spec.alpha :as s]))

(def default-metamask-data-state {:initialized false})

(def default-db
  {:name "re-frame"
   :loader-counter 0
   :metamask-data default-metamask-data-state
   })

(s/def ::initialized boolean?)

(s/def ::metamask-data (s/keys :req-un [::initialized]
                               :opt-un [::network-name ::chosen-account]))

(s/def ::db-spec (s/keys :req-un [::loader-counter ::name ::metamask-data]
                         :opt-un []
                         ))
