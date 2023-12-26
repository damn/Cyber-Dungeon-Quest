(ns context.cursor
  (:require [gdl.context :refer [->cursor mouse-on-stage-actor?]]
            [gdl.scene2d.actor :refer [parent]]
            [utils.core :refer [mapvals]]
            [context.entity.state :as state]
            [cdq.context :refer [set-cursor!]]))

 ;TODO
; * add to trello: moving,etc, clicks not allowed & click -> soft-denied-sound, X cross appearing?
; e.g. I thought clicks don't work but I was moving

; * add to trello: play-sound in cdq you send a sound-id not file ?
; (same?)

; * ask the cursor creator in FB !

(extend-type gdl.context.Context
  cdq.context/Cursor
  (set-cursor! [{:keys [context/cursors] :as ctx} cursor-key]
    ; TODO assert , safe-get
    (gdl.context/set-cursor! ctx (get cursors cursor-key))))

#_(defn- button-class? [actor]
  (some #(= com.badlogic.gdx.scenes.scene2d.ui.Button %)
        (supers (class actor))))

#_(defn- button? [actor]
  (or (button-class? actor)
      (and (parent actor)
           (button-class? (parent actor)))))

; TODO add to _all_ screens / or just on enter set default?
; => many screens dont have on-enter anymore.
#_(if-let [actor (mouse-on-stage-actor? ctx)]
    (if (button? actor)
      :button
      :default)
    :default)

(comment

 (instance?(supers (class (.getParent (mouse-on-stage-actor? @gdl.app/current-context)))))
 ;#object[com.badlogic.gdx.scenes.scene2d.ui.Button 0xa829953 "Label: Start game"]
 ; ui/label ... ?
 )

; TODO dispose cursors ( do @ gdl ? )
(defn ->context [ctx]
  {:context/cursors (->> {:cursors/default ["default" 0 0]
                          :cursors/black-x ["black_x" 0 0]
                          :cursors/denied ["denied" 16 16]
                          :cursors/hand ["hand" 4 16]
                          :cursors/sandclock ["sandclock" 16 16]
                          :cursors/walking ["walking" 16 16]
                          :cursors/no-skill-selected ["denied003" 0 0]
                          :cursors/use-skill ["pointer004" 0 0]
                          :cursors/skill-not-usable ["x007" 0 0]
                          :cursors/bag ["bag001" 0 0]}
                         (mapvals (fn [[file x y]]
                                    (->cursor ctx (str "cursors/" file ".png") x y))))})
