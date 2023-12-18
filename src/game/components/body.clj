(ns game.components.body
  (:require [x.x :refer [defcomponent]]
            [gdl.protocols :refer [draw-rectangle]]
            [gdl.math.geom :as geom]
            [gdl.math.vector :as v]
            [game.entity :as entity]
            [game.maps.cell-grid :as grid])
  (:import com.badlogic.gdx.graphics.Color))

(defn- remove-from-occupied-cells! [e]
  (doseq [cell (:occupied-cells @e)]
    (swap! cell update :occupied disj e)))

(defn- set-occupied-cells! [cell-grid e]
  (let [cells (grid/rectangle->occupied-cells cell-grid (:body @e))]
    (doseq [cell cells]
      (swap! cell update :occupied conj e))
    (swap! e assoc :occupied-cells cells)))

(defn- set-touched-cells! [e new-cells]
  {:pre [(not-any? nil? new-cells)]}
  (swap! e assoc :touched-cells new-cells)
  (doseq [cell new-cells]
    (grid/add-entity! cell e)))

(defn- remove-from-touched-cells! [e]
  (doseq [cell (:touched-cells @e)]
    (grid/remove-entity! cell e)))

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
(defn valid-position? [{:keys [context/world-map]} entity*]
  (let [touched-cells (grid/rectangle->touched-cells (:cell-grid world-map)
                                                     (:body entity*))]
    (and
     (not-any? #(grid/cell-blocked? % entity*) touched-cells)
     (or (not (:is-solid (:body entity*)))
         (->> touched-cells
              grid/get-entities-from-cells
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
  (entity/create! [_ e {:keys [context/world-map]}]
    (assert (:position @e))
    ;(assert (valid-position? context @e)) ; TODO error because projectiles do not have left-bottom !
    (swap! e assoc-left-bottom)
    (let [cell-grid (:cell-grid world-map)]
      (set-touched-cells! e (grid/rectangle->touched-cells cell-grid (:body @e)))
      (when is-solid
        (set-occupied-cells! cell-grid e))))
  (entity/destroy! [_ e _ctx]
    (remove-from-touched-cells! e)
    (when is-solid
      (remove-from-occupied-cells! e)))
  (entity/moved! [_ e {:keys [context/world-map] :as context} direction-vector]
    (assert (valid-position? context @e))
    (when rotate-in-movement-direction?
      (swap! e assoc-in [:body :rotation-angle] (v/get-angle-from-vector direction-vector)))
    (update-touched-cells! e (grid/rectangle->touched-cells (:cell-grid world-map)
                                                            (:body @e)))
    (when is-solid
      (remove-from-occupied-cells! e)
      (set-occupied-cells! (:cell-grid world-map) e)))
  (entity/render-debug [_ c e*]
    (when show-body-bounds
      (draw-bounds c body))))
