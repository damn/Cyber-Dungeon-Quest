(ns cdq.context.config
  (:require [core.component :as component]
            [gdl.context :as ctx]))

(component/def :context/config {}
  {:keys [tag configs]}
  (ctx/create [_ _ctx]
    (get configs tag)))
