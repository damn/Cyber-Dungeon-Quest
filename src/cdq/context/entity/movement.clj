(ns cdq.context.entity.movement
  (:require [x.x :refer [defcomponent]]
            [gdl.math.geom :as geom]
            [gdl.math.vector :as v]
            [utils.core :refer [find-first]]
            [cdq.context.ecs :as ecs]
            [cdq.context :refer [do-effect! world-grid position-changed!]]
            [cdq.context.entity.body :as body]
            [cdq.world.grid :refer [rectangle->cells valid-position?]]
            [cdq.world.cell :as cell :refer [cells->entities]]))

(def frames-per-second 60)
(def max-delta-time 0.04)

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ body/min-solid-body-size max-delta-time))

; for adding speed multiplier modifier -> need to take max-speed into account!
(defn- update-position [entity* delta direction-vector]
  (let [speed (:entity/movement entity*)
        apply-delta (fn [position]
                      (mapv #(+ %1 (* %2 speed delta)) position direction-vector))]
    (-> entity*
        (update :entity/position apply-delta)
        (update-in [:entity/body :left-bottom] apply-delta))))

; TODO DRY with valid-position?
(defn- update-position-projectile! [{:keys [context/delta-time] :as ctx}
                                    projectile
                                    direction]
  ; TODO only 1 swap
  (swap! projectile update-position delta-time direction)
  (let [{:keys [hit-effect
                already-hit-bodies
                piercing]} (:entity/projectile-collision @projectile)
        grid (world-grid ctx)
        cells* (map deref (rectangle->cells grid (:entity/body @projectile)))
        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                     (not= (:entity/faction @projectile)
                                           (:entity/faction @%))
                                     (:solid? (:entity/body @%))
                                     (geom/collides? (:entity/body @projectile)
                                                     (:entity/body @%)))
                               (cells->entities cells*))
        blocked? (cond hit-entity
                       (do
                        (swap! projectile update-in [:entity/projectile-collision :already-hit-bodies] conj hit-entity)
                        (do-effect! (merge ctx {:effect/source projectile
                                                :effect/target hit-entity})
                                    hit-effect)
                        (not piercing))
                       (some #(cell/blocked? % @projectile) cells*)
                       true)]
    (if blocked?
      (do (swap! projectile assoc :entity/destroyed? true) false) ; not moved
      true))) ; moved

(defn- try-move! [{:keys [context/delta-time] :as ctx} entity direction]
  (let [entity* (update-position @entity delta-time direction)]
    (when (valid-position? (world-grid ctx) entity*)
      (reset! entity entity*)
      true)))

(defn- update-position-solid! [ctx entity {vx 0 vy 1 :as direction}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move! ctx entity direction)
        (try-move! ctx entity [xdir 0])
        (try-move! ctx entity [0 ydir]))))

(defcomponent :entity/movement tiles-per-second
  (ecs/create! [_ entity _ctx]
    (assert (and (:entity/body @entity)
                 (:entity/position @entity)))
    (assert (<= tiles-per-second max-speed)))

  (ecs/tick! [_ entity ctx]
    (when-let [direction (:entity/movement-vector @entity)]
      (assert (or (zero? (v/length direction))
                  (v/normalised? direction)))
      (when-not (zero? (v/length direction))
        (when (if (:entity/projectile-collision @entity)
                (update-position-projectile! ctx entity direction)
                (update-position-solid!      ctx entity direction))
          (when (:rotate-in-movement-direction? (:entity/body @entity))
            (swap! entity assoc-in [:entity/body :rotation-angle] (v/get-angle-from-vector direction)))
          (position-changed! ctx entity))))))
