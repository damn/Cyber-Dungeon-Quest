(ns game.ui.entity-info-window
  (:require [gdl.scene2d.ui :as ui]
            [game.ui.mouseover-entity :refer (get-mouseover-entity)])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- entity-info-text []
  (when-let [entity (get-mouseover-entity)]
    (binding [*print-level* nil]
      (with-out-str
       (clojure.pprint/pprint
        (assoc (select-keys
                @entity
                [:id
                 :name
                 :speed
                 :hp
                 :mana
                 :faction
                 :creature
                 :level
                 :is-flying
                 :active-skill?
                 :modifiers
                 :items])
               :skills
               (keys (:skills @entity))))))))

(defn create []
  (let [window (ui/window :title "Info"
                          :id :entity-info-window)
        label (ui/label (entity-info-text))]
    (.expand (.add window label))
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.add window (proxy [Actor] []
                   (act [_delta]
                     (ui/set-text label (entity-info-text))
                     (.pack window))))
    window))
