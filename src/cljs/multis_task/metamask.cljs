(ns multis-task.metamask
  (:require
   [re-frame.core :as re-frame]
   ["ethers" :as ethers]
   [multis-task.util.js-promise :as js-promise]))

(re-frame/reg-cofx
 ::load-api
 (fn [cofx _]
   ;; https://docs.ethers.io/v5/getting-started/#getting-started--connecting
   (if (and ethers
            ethers/providers
            ethers/providers.Web3Provider
            js/window
            (.-ethereum js/window))
     (let [provider (ethers/providers.Web3Provider. (.-ethereum js/window))]
       (assoc cofx
              ::provider provider
              ::signer (.getSigner provider)))
     cofx)))

;; ----- Metamask-specific Effects -----

(re-frame/reg-fx
 ::try-activate
 (fn [promise-args]
   ;; https://docs.ethers.io/v5/getting-started/#getting-started--connecting
   (let [ethjs (.-ethereum js/window)]
     (js-promise/wrap-to-dispatch
      (.enable ethjs)
      promise-args))))
