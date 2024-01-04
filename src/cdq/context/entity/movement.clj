(ns cdq.context.entity.movement
  (:require [x.x :refer [defcomponent]]
            [gdl.math.vector :as v]
            [cdq.context.ecs :as ecs]
            [cdq.context :refer [world-grid position-changed!]]
            [cdq.context.entity.body :as body]
            [cdq.world.grid :refer [valid-position?]]))

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

(defn- update-position-non-solid [{:keys [context/delta-time] :as ctx} entity* direction]
  (update-position entity* delta-time direction))

(defn- try-move [{:keys [context/delta-time] :as ctx} entity* direction]
  (let [entity* (update-position entity* delta-time direction)]
    (when (valid-position? (world-grid ctx) entity*) ; TODO call on ctx shortcut fn
      entity*)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
(defn- update-position-solid [ctx entity* {vx 0 vy 1 :as direction}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move ctx entity* direction)
        (try-move ctx entity* [xdir 0])
        (try-move ctx entity* [0 ydir]))))

(defn- check-rotate [entity* direction]
  (if (:rotate-in-movement-direction? (:entity/body entity*))
    (assoc-in entity* [:entity/body :rotation-angle] (v/get-angle-from-vector direction))
    entity*))

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
        (when-let [moved-entity* (if (:solid? (:entity/body @entity))
                                   (update-position-solid     ctx @entity direction)
                                   (update-position-non-solid ctx @entity direction))]
          (reset! entity (check-rotate moved-entity* direction))
          (position-changed! ctx entity))))))
