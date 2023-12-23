(ns context.ui.entity-info-window
  (:require [gdl.context :refer [->actor]]
            [gdl.scene2d.ui :as ui]
            [game.entity :as entity]))

(defn- entity-info-text [entity*]
  (binding [*print-level* nil]
    (with-out-str
     (clojure.pprint/pprint
      {:id (:id entity*)
       :state (entity/state entity*)}))))

(defn create [context]
  (let [window (ui/window :title "Info"
                          :id :entity-info-window)
        label (ui/label "")]
    (.expand (.add window label))
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.add window (->actor context
                          {:act (fn [context]
                                  (ui/set-text label
                                               (when-let [entity @(:context/mouseover-entity context)]
                                                 (entity-info-text @entity)))
                                  (.pack window))}))
    window))
