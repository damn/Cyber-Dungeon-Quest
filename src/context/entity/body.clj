(ns context.entity.body
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-rectangle]]
            [gdl.graphics.color :as color]
            [gdl.math.vector :as v]
            [context.entity :as entity]
            [cdq.context :refer [world-grid]]
            [cdq.world.grid :refer [add-entity! remove-entity! entity-position-changed!]]))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
(def min-solid-body-size 0.4)

(defn- draw-bounds [c {[x y] :left-bottom :keys [width height is-solid]}]
  (draw-rectangle c x y width height (if is-solid color/white color/gray)))

(defn assoc-left-bottom [{:keys [entity/body] [x y] :position :as entity*}]
  (assoc-in entity* [:entity/body :left-bottom] [(- x (/ (:width body)  2))
                                                 (- y (/ (:height body) 2))]))

(def show-body-bounds false)

(defrecord Body [width
                 height
                 half-width
                 half-height
                 radius
                 is-solid
                 rotation-angle
                 rotate-in-movement-direction?])

(defcomponent :entity/body {:keys [left-bottom
                                   width
                                   height
                                   is-solid
                                   rotation-angle
                                   rotate-in-movement-direction?] :as body}
  (entity/create [_]
    (assert (and width height
                 (>= width  (if is-solid min-solid-body-size 0))
                 (>= height (if is-solid min-solid-body-size 0))
                 (boolean? is-solid)
                 (or (nil? rotation-angle)
                     (<= 0 rotation-angle 360))))
    (map->Body
     {:left-bottom left-bottom
      :width  (float width)
      :height (float height)
      :half-width  (float (/ width  2))
      :half-height (float (/ height 2))
      :radius (float (max (/ width  2)
                          (/ height 2)))
      :is-solid is-solid
      :rotation-angle (or rotation-angle 0)
      :rotate-in-movement-direction? rotate-in-movement-direction?}))

  (entity/create! [_ entity context]
    ; TODO VALID POSITION CHECK (done @ world-grid?)
    (assert (:position @entity))
    (swap! entity assoc-left-bottom)
    (add-entity! (world-grid context) entity))

  (entity/destroy! [_ entity context]
    (remove-entity! (world-grid context) entity))

  (entity/moved! [_ entity context direction-vector]
    (when rotate-in-movement-direction?
      (swap! entity assoc-in [:entity/body :rotation-angle] (v/get-angle-from-vector direction-vector)))
    (entity-position-changed! (world-grid context) entity))

  (entity/render-debug [_ e* context]
    (when show-body-bounds
      (draw-bounds context body))))
