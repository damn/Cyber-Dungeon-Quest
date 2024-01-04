(ns cdq.context.entity.projectile-collision
  (:require [x.x :refer [defcomponent]]
            [gdl.math.geom :as geom]
            [utils.core :refer [find-first]]
            [cdq.context.ecs :as ecs]
            [cdq.context :refer [do-effect! world-grid]]
            [cdq.context.entity.body :as body]
            [cdq.world.grid :refer [rectangle->cells]]
            [cdq.world.cell :as cell :refer [cells->entities]]))

(defcomponent :entity/projectile-collision {:keys [hit-effect
                                                   already-hit-bodies
                                                   piercing]}
  (ecs/tick! [[k _] entity ctx]
    (let [entity* @entity
          cells* (map deref (rectangle->cells (world-grid ctx) (:entity/body entity*)))
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
          destroy? (cond hit-entity
                         (do ; TODO? passed entity does not have new position/hit bodies
                             (do-effect! (merge ctx {:effect/source entity
                                                     :effect/target hit-entity})
                                         hit-effect)
                             (not piercing))
                         (some #(cell/blocked? % entity*) cells*)
                         true)]
      (reset! entity (if destroy?
                       (assoc entity* :entity/destroyed? true)
                       entity*)))))
