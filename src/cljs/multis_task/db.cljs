(ns multis-task.db
  (:require [clojure.spec.alpha :as s]))

(def default-metamask-data-state {:initialized false})
(def default-route-state :home)

(def default-db
  {:loader-counter 0
   :metamask-data default-metamask-data-state
   :route default-route-state
   :ui-errors []
   :field-errors {}
   :token-stream-form {}
   :form-invalidation {}
   })

(s/def ::initialized boolean?)

(s/def ::route #{:home :initiate-token-stream})

(s/def ::metamask-data (s/keys :req-un [::initialized]
                               :opt-un [::network-name ::chosen-account]))

(s/def ::token-stream-form (s/keys :opt-un [::erc20-token-name ::erc20-token-addr ::date-from ::time-from]))

(s/def ::field-errors (s/keys :opt-un [::erc20-token-addr-input ::token-stream-form]))

(s/def ::form-invalidation (s/keys :opt-un []))

(s/def ::db-spec (s/keys :req-un [::loader-counter ::metamask-data ::route ::ui-errors ::field-errors ::form-invalidation]
                         :opt-un []))
