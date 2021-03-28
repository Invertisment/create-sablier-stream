(ns multis-task.util.js-promise
  (:require
   [re-frame.core :as re-frame]))

(defn wrap-to-dispatch [promise {:keys [on-success on-fail on-finally] :as promise-args}]
  (cond-> promise
    on-success (.then #(re-frame/dispatch on-success))
    on-fail (.catch (fn [e]
                      (re-frame/dispatch (concat on-fail [e]))))
    on-finally (.finally #(re-frame/dispatch on-finally))))
