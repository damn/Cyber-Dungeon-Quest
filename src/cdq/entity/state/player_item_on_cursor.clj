(ns cdq.entity.state.player-item-on-cursor
  (:require [gdl.context :refer [play-sound! mouse-on-stage-actor? button-just-pressed? draw-centered-image
                                 world-mouse-position gui-mouse-position]]
            [gdl.input.buttons :as buttons]
            [gdl.math.vector :as v]
            [cdq.entity.state :as state]
            [cdq.entity.state.player-idle :refer [click-distance-tiles]]
            [cdq.context :refer [item-entity]]
            [cdq.entity :as entity]))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.

(defn- put-item-on-ground! [{:keys [context/player-entity] :as context} position]
  {:pre [(:entity/item-on-cursor @player-entity)]}
  (play-sound! context "sounds/bfxr_itemputground.wav")
  (item-entity context position (:entity/item-on-cursor @player-entity)))

(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn- item-place-position [ctx entity]
  (placement-point (:entity/position @entity)
                   (world-mouse-position ctx)
                   ; so you cannot put it out of your own reach
                   (- cdq.entity.state.player-idle/click-distance-tiles 0.1)))

(defn- world-item? [ctx]
  (not (mouse-on-stage-actor? ctx)))

(defrecord PlayerItemOnCursor [entity item]
  state/PlayerState
  (player-enter [_])
  (pause-game? [_] true)

  (manual-tick [_ context]
    (when (and (button-just-pressed? context buttons/left)
               (world-item? context))
      [[:tx/event entity :drop-item]]))

  state/State
  (enter [_ _ctx]
    [[:tx/cursor :cursors/hand-grab]
     [(assoc @entity :entity/item-on-cursor item)]])

  (exit [_ ctx]
    ; at context.ui.inventory-window/clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (when (:entity/item-on-cursor @entity)
      (put-item-on-ground! ctx (item-place-position ctx entity))
      (swap! entity dissoc :entity/item-on-cursor)
      nil))

  (tick [_ _ctx])
  (render-below [_ ctx entity*]
    (when (world-item? ctx)
      (draw-centered-image ctx (:property/image item) (item-place-position ctx entity))))
  (render-above [_ ctx entity*])
  (render-info  [_ ctc entity*]))

(defn draw-item-on-cursor [{:keys [context/player-entity] :as context}]
  (when (and (= :item-on-cursor (entity/state @player-entity))
             (not (world-item? context)))
    (draw-centered-image context
                         (:property/image (:entity/item-on-cursor @player-entity))
                         (gui-mouse-position context))))