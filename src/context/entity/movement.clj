(ns context.entity.movement
  (:require [x.x :refer [defcomponent doseq-entity]]
            [gdl.math.geom :as geom]
            [gdl.math.vector :as v]
            [utils.core :refer [find-first]]
            [context.entity :as entity]
            [cdq.context :refer [do-effect! audiovisual world-grid]]
            [context.entity.body :as body]
            [cdq.world.grid :refer [rectangle->cells valid-position?]]
            [cdq.world.cell :as cell :refer [cells->entities]]))

; TODO
; * max speed @ creation not checked & with movement speed modifiers @ update-position

; das spiel soll bei 20fps noch "schnell" sein,d.h. net langsamer werden (max-delta wirkt -> game wird langsamer)
; TODO makes no sense why should it be fast then
; 1000/20 = 50
(def max-delta 50)

; max speed damit kleinere bodies beim direkten dr�berfliegen nicht �bersprungen werden (an den ecken werden sie trotzdem �bersprungen..)
; schnellere speeds m�ssten in mehreren steps sich bewegen.
(def ^:private max-speed (* 1000 (/ body/min-solid-body-size max-delta))) ; TODO is not checked
; => world-units / second

; TODO check max-speed for not skipping min-size-bodies
; (* speed multiplier delta )
; probably fixed timestep 17 ms or something
; if speed > max speed, then make multiple smaller updates
; -> any kind of speed possible (fast arrows)
(defn- update-position [entity* delta direction-vector]
  (let [speed (:entity/movement entity*)
        apply-delta (fn [position]
                      (mapv #(+ %1 (* %2 speed delta)) position direction-vector))]
    (-> entity*
        (update :position apply-delta)
        (update-in [:body :left-bottom] apply-delta))))

; TODO DRY with valid-position?
(defn- update-position-projectile! [{:keys [context/delta-time] :as ctx}
                                    projectile
                                    direction]
  (swap! projectile update-position delta-time direction)
  (let [{:keys [hit-effect
                already-hit-bodies
                piercing]} (:projectile-collision @projectile)
        grid (world-grid ctx)
        cells (rectangle->cells grid (:body @projectile))
        hit-entity (find-first #(and (not (contains? already-hit-bodies %))
                                     (not= (:faction @projectile) (:faction @%))
                                     (:is-solid (:body @%))
                                     (geom/collides? (:body @projectile) (:body @%)))
                               (cells->entities (map deref cells)))
        blocked (cond hit-entity
                      (do
                       (swap! projectile update-in [:projectile-collision :already-hit-bodies] conj hit-entity)
                       (do-effect! (merge ctx {:effect/source projectile
                                               :effect/target hit-entity})
                                   hit-effect)
                       (not piercing))
                      (some #(cell/blocked? @% @projectile) cells)
                      (do
                       (audiovisual ctx (:position @projectile) :projectile/hit-wall-effect)
                       true))]
    (if blocked
      (do
       (swap! projectile assoc :destroyed? true)
       false) ; not moved
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

(defcomponent :entity/movement speed-in-seconds
  (entity/create! [[k _] entity _ctx]
    (assert (and (:body     @entity)
                 (:position @entity)))
    (swap! e assoc k (/ speed-in-seconds 1000)))

  (entity/tick! [_ entity ctx]
    (when-let [direction (:entity/movement-vector @e)]
      (assert (or (zero? (v/length direction)) ; TODO what is the point of zero length vectors?
                  (v/normalised? direction)))
      (when-not (zero? (v/length direction))
        (when-let [moved? (if (:projectile-collision @entity)
                            (update-position-projectile! ctx entity direction)
                            (update-position-solid!      ctx entity direction))]
          (doseq-entity entity
                        entity/moved!
                        ctx
                        direction))))))
