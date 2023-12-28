(ns context.entity.state.player-item-on-cursor
  (:require [gdl.context :refer [play-sound! mouse-on-stage-actor? button-just-pressed? draw-centered-image
                                 world-mouse-position gui-mouse-position]]
            [gdl.input.buttons :as buttons]
            [gdl.math.vector :as v]
            [context.entity.state :as state]
            [cdq.context :refer [item-entity send-event! set-cursor!]]
            [cdq.entity :as entity]))

; => placed it in not in line of sight tile -> hidden ! because not corner check for los show
; => its actually OK if they are not valid position , put on rocks, etc. is fine
; => its also funny its like you threw your item up the hill...
; can put somewhere where can't click -> little bit less than max click range make
; * put on player mouseover => move back to inventory?

(defn- put-item-on-ground! [{:keys [context/player-entity] :as context} position]
  {:pre [(:item-on-cursor @player-entity)]}
  (play-sound! context "sounds/bfxr_itemputground.wav")
  (item-entity context position (:item-on-cursor @player-entity)))

(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn- item-place-position [ctx entity]
  (placement-point (:position @entity) (world-mouse-position ctx) 1.5))

(defrecord PlayerItemOnCursor [entity item]
  state/PlayerState
  (player-enter [_ _ctx])
  (pause-game? [_] true)

  (manual-tick! [_ context delta]
    (when (and (button-just-pressed? context buttons/left)
               (not (mouse-on-stage-actor? context)))
      (send-event! context entity :drop-item)))

  state/State
  (enter [_ ctx]
    (set-cursor! ctx :cursors/hand-grab)
    (swap! entity assoc :item-on-cursor item))

  (exit [_ ctx]
    ; at context.ui.inventory-window/clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (when (:item-on-cursor @entity)
      (put-item-on-ground! ctx (item-place-position ctx entity))
      (swap! entity dissoc :item-on-cursor)))

  (tick! [_ _ctx _delta])
  (render-below [_ ctx entity*]
    (when (not (mouse-on-stage-actor? ctx))
      (draw-centered-image ctx (:image item) (item-place-position ctx entity))))
  (render-above [_ ctx entity*])
  (render-info  [_ ctc entity*]))

(defn draw-item-on-cursor [{:keys [context/player-entity] :as context}]
  (when (and (= :item-on-cursor (entity/state @player-entity))
             (mouse-on-stage-actor? context))
    (draw-centered-image context
                         (:image (:item-on-cursor @player-entity))
                         (gui-mouse-position context))))
