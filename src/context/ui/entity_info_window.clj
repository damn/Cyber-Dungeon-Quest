(ns context.ui.entity-info-window
  (:require [gdl.context :refer [->actor ->window ->label]]
            [gdl.scene2d.ui.label :refer [set-text!]]
            [gdl.scene2d.group :refer [add-actor!]]
            [cdq.entity :as entity]))

(defn- entity-info-text [entity*]
  (binding [*print-level* nil]
    (with-out-str
     (clojure.pprint/pprint
      {:id (:id entity*)
       :state (entity/state entity*)}))))

(defn create [context]
  (let [label (->label context "")
        window (->window context {:title "Info"
                                  :id :entity-info-window
                                  :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (add-actor! window (->actor context
                                {:act (fn [context]
                                        (set-text! label
                                                   (when-let [entity @(:context/mouseover-entity context)]
                                                     (entity-info-text @entity)))
                                        (.pack window))}))
    window))
