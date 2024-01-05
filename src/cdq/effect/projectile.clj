(ns cdq.effect.projectile
  (:require [clojure.string :as str]
            [malli.core :as m]
            [gdl.math.vector :as v]
            [gdl.graphics.animation :as animation]
            [gdl.context :refer [get-sprite spritesheet]]
            [cdq.context :refer [effect-text path-blocked?]]
            [cdq.effect :as effect]))

; -> range needs to be smaller than potential field range
; -> first range check then ray ! otherwise somewhere in contentfield out of sight

(def ^:private size 0.5)
(def ^:private maxrange 10)
(def ^:private speed 10)
(def ^:private maxtime (/ maxrange speed))

(def ^:private schema
  (m/schema [:= true]))

(defmethod effect/value-schema :effect/projectile [_]
  schema)

; TODO valid params direction has to be  non-nil (entities not los player ) ?
(defmethod effect/useful? :effect/projectile
  [{:keys [effect/source
           effect/target] :as context}
   _effect]
  (and (not (path-blocked? context
                           (:entity/position source) ; TODO test
                           (:entity/position target)
                           size))
       ; TODO not taking into account body sizes
       (< (v/distance (:entity/position source)
                      (:entity/position target))
          maxrange)))

(comment
 ; for chance to do hit-effect -> use this code @ hit-effects
 ; (extend hit-effects with chance , not effects themself)
 ; and hit-effects to text ...hmmm
 [utils.random :as random]
 (or (not chance)
     (random/percent-chance chance)))

(def ^:private hit-effect
  [[:effect/damage [:magic [4 8]]]
   [:effect/stun 0.5]
   ;[:stun {:duration 0.2} {:chance 100}]
   ])

(defn- black-projectile [context]
  (animation/create [(get-sprite context
                                 (spritesheet context "fx/uf_FX.png" 24 24)
                                 [1 12])]
                    :frame-duration 0.5))

(defmethod effect/text :effect/projectile
  [context _effect]
  (effect-text context hit-effect))

(defmethod effect/valid-params? :effect/projectile
  [{:keys [effect/source
           effect/target
           effect/direction]}
   _effect]
  (and source target direction)) ; faction @ source also ?

(defn- start-point [entity* direction]
  (v/add (:entity/position entity*)
         (v/scale direction
                  (+ (:radius (:entity/body entity*)) size 0.1))))

(defmethod effect/transactions :effect/projectile
  [{:keys [effect/source
           effect/direction] :as context}
   _effect]
  [#:entity {:position (start-point source direction)
             :faction (:entity/faction source)
             :body {:width size
                    :height size
                    :solid? false
                    :rotation-angle (v/get-angle-from-vector direction)}
             :flying? true
             :z-order :z-order/effect
             :movement speed
             :movement-vector direction
             :animation (black-projectile context)
             :delete-after-duration maxtime
             :plop true
             :projectile-collision {:hit-effect hit-effect
                                    :piercing? true}}])
