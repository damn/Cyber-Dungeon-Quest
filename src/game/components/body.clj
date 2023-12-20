(ns game.components.body
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-rectangle]]
            [gdl.math.geom :as geom]
            [gdl.math.vector :as v]
            [context.ecs :as entity]
            [game.context :refer [get-cell-grid]]
            [game.world.cell-grid :refer [rectangle->touched-cells]]
            [game.world.cell :as cell])
  (:import com.badlogic.gdx.graphics.Color))

(defn- remove-from-occupied-cells! [entity]
  (doseq [cell (:occupied-cells @entity)]
    (swap! cell cell/remove-occupying-entity entity)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- rectangle->occupied-cells [cell-grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> width 1) (> height 1))
    (rectangle->touched-cells cell-grid rectangle)
    [(get cell-grid
          [(int (+ (left-bottom 0) (/ width 2)))
           (int (+ (left-bottom 1) (/ height 2)))])]))

(defn- set-occupied-cells! [cell-grid e]
  (let [cells (rectangle->occupied-cells cell-grid (:body @e))]
    (doseq [cell cells]
      (swap! cell cell/add-occupying-entity e))
    (swap! e assoc :occupied-cells cells)))

(defn- set-touched-cells! [e new-cells]
  {:pre [(not-any? nil? new-cells)]}
  (swap! e assoc :touched-cells new-cells)
  (doseq [cell new-cells]
    (swap! cell cell/add-entity entity)))

(defn- remove-from-touched-cells! [e]
  (doseq [cell (:touched-cells @e)]
    (swap! cell cell/remove-entity entity)))

(defn- update-touched-cells! [e touched-cells]
  (when-not (= touched-cells (:touched-cells @e))
    (remove-from-touched-cells! e)
    (set-touched-cells! e touched-cells)))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
(def min-solid-body-size 0.3)

(defn- draw-bounds [c {[x y] :left-bottom :keys [width height is-solid]}]
  (draw-rectangle c x y width height (if is-solid Color/WHITE Color/GRAY)))

; TODO DELETE NOW !!! fucks up everything & confusing as fuck,
; make into function idk ?
(defn assoc-left-bottom [{:keys [body] [x y] :position :as entity*}]
  (assoc-in entity* [:body :left-bottom] [(- x (/ (:width body)  2))
                                          (- y (/ (:height body) 2))]))

; needs only cell-grid actually? or protocol ol world-map?
; ON WORLD (not world-map) world/valid-position?
(defn valid-position? [context entity*]
  ; TODO save params & check why its not a valid position

  (let [touched-cells (rectangle->touched-cells (get-cell-grid context)
                                                (:body entity*))]
    (and
     (not-any? #(cell/blocked? @% entity*) touched-cells)
     (or (not (:is-solid (:body entity*)))
         (->> touched-cells
              (map deref)
              cells->entities
              (not-any? #(and (not= (:id @%) (:id entity*))
                              (:is-solid (:body @%))
                              (geom/collides? (:body @%) (:body entity*)))))))))

(def show-body-bounds false)

(defrecord Body [width
                 height
                 half-width
                 half-height
                 radius
                 is-solid
                 rotation-angle
                 rotate-in-movement-direction?])

(defcomponent :body {:keys [left-bottom width height is-solid rotation-angle rotate-in-movement-direction?] :as body}
  (entity/create [_]
    (assert (and width height
                 (>= width  (if is-solid min-solid-body-size 0))
                 (>= height (if is-solid min-solid-body-size 0))
                 (boolean? is-solid)
                 (or (nil? rotation-angle)
                     (<= 0 rotation-angle 360))))
    (map->Body
     {:left-bottom left-bottom
      :width width
      :height height
      :half-width  (/ width  2)
      :half-height (/ height 2)
      :radius (max (/ width  2)
                   (/ height 2))
      :is-solid is-solid
      :rotation-angle (or rotation-angle 0)
      :rotate-in-movement-direction? rotate-in-movement-direction?}))
  (entity/create! [_ e context]
    (assert (:position @e))
    ;(assert (valid-position? context @e)) ; TODO error because projectiles do not have left-bottom !
    (swap! e assoc-left-bottom)
    (let [cell-grid (get-cell-grid context)]
      (set-touched-cells! e (rectangle->touched-cells cell-grid (:body @e)))
      (when is-solid
        (set-occupied-cells! cell-grid e))))
  (entity/destroy! [_ e _ctx]
    (remove-from-touched-cells! e)
    (when is-solid
      (remove-from-occupied-cells! e)))
  (entity/moved! [_ e context direction-vector]
    (assert (valid-position? context @e))
    (when rotate-in-movement-direction?
      (swap! e assoc-in [:body :rotation-angle] (v/get-angle-from-vector direction-vector)))
    (let [cell-grid (get-cell-grid context)]
      (update-touched-cells! e (rectangle->touched-cells cell-grid (:body @e)))
      (when is-solid
        (remove-from-occupied-cells! e)
        (set-occupied-cells! cell-grid e))))
  (entity/render-debug [_ c e*]
    (when show-body-bounds
      (draw-bounds c body))))
