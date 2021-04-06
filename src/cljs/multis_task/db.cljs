(ns multis-task.db
  (:require [clojure.spec.alpha :as s]))

(def default-metamask-data-state {:initialized false})
(def default-route-state :home)

(def default-db
  {:loader-counter 0
   :metamask-data default-metamask-data-state
   :route default-route-state
   :ui-errors []
   :ui-successes []
   :field-errors {}
   :token-stream-form {}
   :form-invalidation {}

   :erc20-sablier-deposit-approval-done false
   })

(s/def ::initialized boolean?)

(s/def ::route #{:home :initiate-token-stream :stream-creation-success})

(s/def ::metamask-data (s/keys :req-un [::initialized]
                               :opt-un [::network-name ::chosen-account]))

(s/def ::token-stream-form (s/keys :opt-un [::erc20-token-name ::erc20-token-decimals ::erc20-token-addr ::date-from ::time-from]))

(s/def ::field-errors (s/keys :opt-un [::erc20-token-addr-input ::token-stream-form]))

(s/def ::form-invalidation (s/keys :opt-un []))

(s/def ::erc20-sablier-deposit-approval-done boolean?)

(s/def ::notification string?)
(s/def ::ui-errors (s/coll-of ::notification))
(s/def ::ui-successes (s/coll-of ::notification))

(s/def ::db-spec
  (s/keys :req-un [::loader-counter
                   ::metamask-data
                   ::route

                   ::erc20-sablier-deposit-approval-done

                   ::ui-errors
                   ::ui-successes

                   ::token-stream-form

                   ::field-errors
                   ::form-invalidation]
          :opt-un []))
