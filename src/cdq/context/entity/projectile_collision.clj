(ns cdq.context.entity.projectile-collision
  (:require [x.x :refer [defcomponent]]
            [gdl.math.geom :as geom]
            [utils.core :refer [find-first]]
            [cdq.context.ecs :as ecs]
            [cdq.context :refer [world-grid]]
            [cdq.context.entity.body :as body]
            [cdq.world.grid :refer [rectangle->cells]]
            [cdq.world.cell :as cell :refer [cells->entities]]))

(defcomponent :entity/projectile-collision {:keys [hit-effect
                                                   already-hit-bodies
                                                   piercing?]}
  (ecs/create [[_ v]]
    (assoc v :already-hit-bodies #{}))

  (ecs/tick [[k _] entity* ctx]
    (let [cells* (map deref (rectangle->cells (world-grid ctx) (:entity/body entity*)))
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (:entity/faction entity*)
                                             (:entity/faction @%))
                                       (:solid? (:entity/body @%))
                                       (geom/collides? (:entity/body entity*)
                                                       (:entity/body @%)))
                                 (cells->entities cells*))
          entity* (if hit-entity
                    (update-in entity* [k :already-hit-bodies] conj hit-entity)
                    entity*)
          destroy? (or (and hit-entity (not piercing?))
                       (some #(cell/blocked? % entity*) cells*))]
      [(if destroy?
         (assoc entity* :entity/destroyed? true)
         entity*)
       (when hit-entity
         ; TODO? passed entity does not have new position/hit bodies
         [:ctx/do-effect
          {:effect/source (:cdq.context.ecs/atom (meta entity*))
           :effect/target hit-entity}
          hit-effect])])))
