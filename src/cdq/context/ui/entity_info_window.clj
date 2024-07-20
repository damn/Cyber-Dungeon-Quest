(ns cdq.context.ui.entity-info-window
  (:require [gdl.context :refer [->actor ->window ->label]]
            [gdl.scene2d.ui.label :refer [set-text!]]
            [gdl.scene2d.group :refer [add-actor!]]
            [gdl.scene2d.ui.widget-group :refer [pack!]]
            [cdq.api.entity :as entity]))

(defn- entity-info-text [entity*]
  (binding [*print-level* nil]
    (with-out-str
     (clojure.pprint/pprint
      {:uid   (:entity/uid entity*)
       :state (entity/state entity*)
       :faction (:entity/faction entity*)
       :stats (:entity/stats entity*)
       }))))

(defn create [{:keys [gui-viewport-width] :as context}]
  (let [label (->label context "")
        window (->window context {:title "Info"
                                  :id :entity-info-window
                                  :visible? false
                                  :position [gui-viewport-width 0]
                                  :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (add-actor! window (->actor context
                                {:act (fn [context]
                                        (set-text! label
                                                   (when-let [entity @(:context/mouseover-entity context)]
                                                     (entity-info-text @entity)))
                                        (pack! window))}))
    window))
