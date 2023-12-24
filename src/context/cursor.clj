(ns context.cursor
  (:require [gdl.context :refer [->actor ->cursor mouse-on-stage-actor?]]))

; cursor states

; * first of all look PLAYER STATE

; then item-on-cursor state & idle mostly, others are 'busy' or 'dead'
; => cursor state keyword namespaced

; item-on-cursor can put the item as cursor directly with hotspot in the middle?

; then
; * mouse-on-stage-actor? -> there check also more stuff dep. on what is hit (button, drag window, idk, inventory can take or not, actionbar )

; * clickable-mouseover-entity?

; * then SKILL USABLE STATE / show skill info if you WOULD do your skill ! or not enough mana etc.
; (draw your own cursors?)


(defn- set-cursor! [{:keys [context/cursors] :as ctx} cursor-key]
  (gdl.context/set-cursor! ctx (get cursors cursor-key)))

(defn- button? [actor]
  (some #(= com.badlogic.gdx.scenes.scene2d.ui.Button %)
        (supers (class actor))))

(defn ->cursor-update-actor [{:keys [context/cursors] :as ctx}]
  (->actor ctx {:act (fn [ctx]
                       #_(set-cursor! ctx
                                    (if-let [actor (mouse-on-stage-actor? ctx)]
                                      (cond (button? actor)
                                            :mouse-on-stage-actor?
                                            (.getParent actor)
                                            (if (button? (.getParent actor))
                                              :mouse-on-stage-actor?
                                              :default)
                                            :else
                                            :default)
                                      :default)))}))

(comment

 (instance?(supers (class (.getParent (mouse-on-stage-actor? @gdl.app/current-context)))))
 ;#object[com.badlogic.gdx.scenes.scene2d.ui.Button 0xa829953 "Label: Start game"]
 ; ui/label ... ?
 )

(defn ->context [ctx]
  (let [cursors {:mouse-on-stage-actor?  (->cursor ctx "button.png")
                 :default (->cursor ctx "default.png")}]
    {:context/cursors cursors}))

; all screens cursor actor ....
; conj @ app.start to :actors ?

; _all_ screens have to have a cursor actor => _CdqScreen_ ?
; otherwise we keep the last selected cursor (e.g. mouse over button)

; FIXME buttons not working and image not stretched all the way?
;=> for image use a widget and pass the same image through only once.
