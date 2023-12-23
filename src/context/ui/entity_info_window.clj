(ns context.ui.entity-info-window
  (:require [gdl.app :refer [current-context]]
            [gdl.scene2d.ui :as ui]
            [game.entity :as entity])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- entity-info-text [entity*]
  (binding [*print-level* nil]
    (with-out-str
     (clojure.pprint/pprint
      {:id (:id entity*)
       :state (entity/state entity*)}))))

(defn create []
  (let [window (ui/window :title "Info"
                          :id :entity-info-window)
        label (ui/label "")]
    (.expand (.add window label))
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.add window (proxy [Actor] []
                   (act [_delta]
                     (ui/set-text label
                                  (when-let [entity @(:context/mouseover-entity @current-context)]
                                    (entity-info-text @entity)))
                     (.pack window))))
    window))
