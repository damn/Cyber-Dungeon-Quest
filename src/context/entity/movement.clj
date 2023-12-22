(ns context.entity.movement
  (:require [x.x :refer [defcomponent doseq-entity]]
            [gdl.math.geom :as geom]
            [gdl.math.vector :as v]
            [utils.core :refer [find-first]]
            [context.ecs :as entity]
            [game.context :refer [do-effect! audiovisual world-grid]]
            [context.entity.body :as body]
            [game.world.grid :refer [rectangle->cells valid-position?]]
            [game.world.cell :as cell :refer [cells->entities]]))

; TODO check max-speed for not skipping min-size-bodies
; (* speed multiplier delta )
; probably fixed timestep 17 ms or something
; if speed > max speed, then make multiple smaller updates
; -> any kind of speed possible (fast arrows)
(defn- apply-delta-v [entity* delta v]
  (let [speed (:entity/movement entity*)
        apply-delta (fn [p]
                      (mapv #(+ %1 (* %2 speed delta))
                            p
                            v))]
    (-> entity*
        (update :position apply-delta)
        (update-in [:body :left-bottom] apply-delta))))

; TODO DRY with valid-position?
(defn- update-position-projectile [context projectile delta v]
  (swap! projectile apply-delta-v delta v)
  (let [{:keys [hit-effect
                already-hit-bodies
                piercing]} (:projectile-collision @projectile)
        grid (world-grid context)
        cells (rectangle->cells grid (:body @projectile))
        hit-entity (find-first #(and (not (contains? already-hit-bodies %))
                                     (not= (:faction @projectile) (:faction @%))
                                     (:is-solid (:body @%))
                                     (geom/collides? (:body @projectile) (:body @%)))
                               (cells->entities (map deref cells)))
        blocked (cond hit-entity
                      (do
                       (swap! projectile update-in [:projectile-collision :already-hit-bodies] conj hit-entity)
                       (do-effect! (merge context {:effect/source projectile
                                                   :effect/target hit-entity})
                                  hit-effect)
                       (not piercing))
                      (some #(cell/blocked? @% @projectile) cells)
                      (do
                       (audiovisual context (:position @projectile) :projectile/hit-wall-effect)
                       true))]
    (if blocked
      (do
       (swap! projectile assoc :destroyed? true)
       false) ; not moved
      true))) ; moved

(defn- try-move [grid entity delta v]
  (let [entity* (apply-delta-v @entity delta v)]
    (when (valid-position? grid entity*)
      (reset! entity entity*)
      true)))

(defn- update-position-solid [grid entity delta {vx 0 vy 1 :as v}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid entity delta v)
        (try-move grid entity delta [xdir 0])
        (try-move grid entity delta [0 ydir]))))

; das spiel soll bei 20fps noch "schnell" sein,d.h. net langsamer werden (max-delta wirkt -> game wird langsamer)
; TODO makes no sense why should it be fast then
; 1000/20 = 50
(def max-delta 50)

; max speed damit kleinere bodies beim direkten dr�berfliegen nicht �bersprungen werden (an den ecken werden sie trotzdem �bersprungen..)
; schnellere speeds m�ssten in mehreren steps sich bewegen.
(def ^:private max-speed (* 1000 (/ body/min-solid-body-size max-delta))) ; TODO is not checked
; => world-units / second

(defcomponent :entity/movement speed-in-seconds
  (entity/create! [_ e _ctx]
    (assert (and (:body @e) (:position @e)))
    (swap! e assoc :entity/movement (/ speed-in-seconds 1000)))

  (entity/tick! [_ e context delta]
    (when-let [direction (:movement-vector @e)]
      (assert (or (zero? (v/length direction)) ; TODO what is the point of zero length vectors?
                  (v/normalised? direction)))
      (when-not (zero? (v/length direction))
        (when-let [moved? (if (:projectile-collision @e)
                            (update-position-projectile context e delta direction)
                            (update-position-solid (world-grid context) e delta direction))]
          (doseq-entity e entity/moved! context direction))))))
