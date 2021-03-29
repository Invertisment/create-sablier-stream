(ns multis-task.interceptors
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :as re-frame]
            [multis-task.config :as config]
            [multis-task.db :as db]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

;; now we create an interceptor using `after`
(def check-spec-interceptor (re-frame/after (partial check-and-throw ::db/db-spec)))

(def interceptors (if config/debug?
                    [check-spec-interceptor]
                    []))

