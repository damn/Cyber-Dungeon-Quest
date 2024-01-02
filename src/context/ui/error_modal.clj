(ns context.ui.error-modal
  (:require [gdl.context :refer [->window ->label get-stage]]
            [gdl.scene2d.group :refer [add-actor!]]
            cdq.context))

(extend-type gdl.context.Context
  cdq.context/ErrorModal
  (->error-window [ctx throwable]
    (add-actor! (get-stage ctx)
                (->window ctx {:title "Error"
                               :rows [[(->label ctx (str throwable))]]
                               :modal? true
                               :close-button? true
                               :close-on-escape? true
                               :center? true
                               :pack? true}))))
