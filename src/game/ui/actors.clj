(ns game.ui.actors
  (:require [gdl.context :refer [draw-centered-image gui-mouse-position draw-text]]
            [gdl.scene2d.ui :as ui]
            [gdl.scene2d.actor :as actor]
            [data.counter :as counter]
            [app.state :refer [current-context]]
            [game.context :refer [->player-message-actor]]
            [game.ui.debug-window :as debug-window]
            [game.ui.help-window :as help-window]
            [game.ui.entity-info-window :as entity-info-window]
            [game.ui.skill-window :as skill-window]
            [game.ui.inventory-window :as inventory]
            [game.ui.action-bar :as action-bar])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- draw-item-on-cursor [{:keys [context/player-entity] :as c}]
  (let [{:keys [context/player-entity] :as context} @current-context]
    (when (= :item-on-cursor
             (:state (:fsm (:entity/state @player-entity))))
      (draw-centered-image context
                           (:image (:item-on-cursor @player-entity))
                           (gui-mouse-position context)))))

(defn- item-on-cursor-render-actor []
  (let [actor (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (draw-item-on-cursor @current-context)))]
    (.setName actor "item-cursor")
    (gdl.scene2d.actor/set-touchable actor :disabled)
    actor
    ))

(defn create-actors [{:keys [gui-viewport-width
                             gui-viewport-height]
                      :as context}]
  (let [^Actor debug-window (debug-window/create)
        _ (.setPosition debug-window 0 gui-viewport-height)

        ^Actor help-window (help-window/create)
        _ (.setPosition help-window
                        (- (/ gui-viewport-width 2)
                           (/ (.getWidth help-window) 2))
                        gui-viewport-height)

        ^Actor entity-info-window (entity-info-window/create)
        skill-window (skill-window/create context)]

    (.setPosition inventory/window
                  gui-viewport-width
                  (- (/ gui-viewport-height 2)
                     (/ (.getHeight help-window) 2)))

    (.setPosition entity-info-window (.getX inventory/window) 0)
    (.setWidth entity-info-window (.getWidth inventory/window))
    (.setHeight entity-info-window (.getY inventory/window))

    (comment
     (let [context @app.state/current-context]
       (gdl.context/mouse-on-stage-actor? context)
       )
     )


    ; TODO stack really fucks up the clicks and everything ...
    ; even with touchable idk how to do it and actionbar doesnt respond at all anymore

    (let [stack (ui/stack) ; (actor/set-touchable top-widget :disabled)
          windows (com.badlogic.gdx.scenes.scene2d.Group.)]

      ;(gdl.scene2d.actor/set-touchable windows :disabled) ; now actionbar works
      ; otherwise player clicks also dont work - stage takes over click events !
      (actor/set-id windows :windows-group)
      (.setName windows "windows-group")
      (.addActor windows debug-window)
      (.addActor windows help-window)
      (.addActor windows entity-info-window)
      (.addActor windows inventory/window)
      (.addActor windows skill-window)
      ; 1. action-bar
      (.add stack (let [table (ui/table :rows [[{:actor action-bar/horizontal-group
                                                 :expand? true
                                                 :bottom? true}]]
                                        :fill-parent? true)]
                    (.setName table "actionbar-table")
                    table
                    ))
      ; 2. windows
      (.add stack windows)
      ; 3. cursor item - touchable false
      (.add stack (item-on-cursor-render-actor))
      ; 4. last - player message - touchable false
      (.add stack (->player-message-actor context))
      (.setFillParent stack true)
      [stack])))

; TODO cannot move,click skill window again check stage hit
; otherwise works fine , just add timer


; player message actor - don't draw anything
; just check if message is there then draw
; so use a STACK ?

; 1. action-bar
; 2. windows
; 3. player-message
; 4. item-on-cursor

; but then how to get my windows? check where they are called
; maybe windows group so can close them all
