(ns cdq.entity.body
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-rectangle]]
            [gdl.graphics.color :as color]
            [cdq.entity :as entity]))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
(def min-solid-body-size 0.4)

(defn- draw-bounds [c {[x y] :left-bottom :keys [width height solid?]}]
  (draw-rectangle c x y width height (if solid? color/white color/gray)))

(def show-body-bounds false)

(defrecord Body [width
                 height
                 half-width
                 half-height
                 radius
                 solid?
                 rotation-angle
                 rotate-in-movement-direction?])

(defcomponent :entity/body body
  (entity/create-component [_ {:keys [entity/position]
                               [x y] :entity/position
                               {:keys [left-bottom
                                       width
                                       height
                                       solid?
                                       rotation-angle
                                       rotate-in-movement-direction?]} :entity/body} _ctx]
    (assert position)
    (assert (and width height
                 (>= width  (if solid? min-solid-body-size 0))
                 (>= height (if solid? min-solid-body-size 0))
                 (boolean? solid?)
                 (or (nil? rotation-angle)
                     (<= 0 rotation-angle 360))))
    (map->Body
     {:left-bottom [(- x (/ width  2))
                    (- y (/ height 2))]
      :width  (float width)
      :height (float height)
      :half-width  (float (/ width  2))
      :half-height (float (/ height 2))
      :radius (float (max (/ width  2)
                          (/ height 2)))
      :solid? solid?
      :rotation-angle (or rotation-angle 0)
      :rotate-in-movement-direction? rotate-in-movement-direction?}))

  (entity/render-debug [_ _entity* context]
    (when show-body-bounds
      (draw-bounds context body))))
