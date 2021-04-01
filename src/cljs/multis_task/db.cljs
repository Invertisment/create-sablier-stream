(ns multis-task.db
  (:require [clojure.spec.alpha :as s]))

(def default-metamask-data-state {:initialized false})
(def default-route-state :home)

(def default-db
  {:loader-counter 0
   :metamask-data default-metamask-data-state
   :route default-route-state
   :ui-errors []
   })

(s/def ::initialized boolean?)

(s/def ::route #{:home :connected-menu :initiate-token-stream})

(s/def ::metamask-data (s/keys :req-un [::initialized]
                               :opt-un [::network-name ::chosen-account]))

(s/def ::db-spec (s/keys :req-un [::loader-counter ::metamask-data ::route ::ui-errors]
                         :opt-un []))
