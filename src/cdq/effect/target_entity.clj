(ns cdq.effect.target-entity
  (:require [malli.core :as m]
            [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-line]]
            [gdl.math.vector :as v]
            [cdq.context :refer (effect-text line-of-sight? line-entity)]
            [cdq.effect :as effect]))

(defn- in-range? [entity* target maxrange] ; == circle-collides?
  (< (- (v/distance (:entity/position entity*)
                    (:entity/position target))
        (:radius (:entity/body entity*))
        (:radius (:entity/body target)))
     maxrange))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity* target]
  (v/add (:entity/position entity*)
         (v/scale (v/direction (:entity/position entity*)
                               (:entity/position target))
                  (:radius (:entity/body entity*)))))

(defn- end-point [entity* target maxrange]
  (v/add (start-point entity* target)
         (v/scale (v/direction (:entity/position entity*)
                               (:entity/position target))
                  maxrange)))

(def ^:private schema
  (m/schema [:map [:hit-effect [:map]] [:maxrange pos?]]))

(defcomponent :effect/target-entity {:keys [maxrange hit-effect]}
  (effect/value-schema [_]
    schema)

  (effect/text [_ ctx]
    (str "Range " maxrange " meters\n" (effect-text ctx hit-effect)))

  ; TODO target still exists ?! necessary ? what if disappears/dead?
  ; TODO this is valid-params of hit-effect damage !!
  (effect/valid-params? [_ {:keys [effect/source effect/target] :as ctx}]
    (and source
         target
         (line-of-sight? ctx source target)
         (:entity/hp target)))

  (effect/useful? [_ {:keys [effect/source effect/target]}]
    (in-range? source target maxrange))

  (effect/transactions [_ {:keys [effect/source effect/target] :as ctx}]
    (if (in-range? source target maxrange)
      [[:tx/create (line-entity ctx
                                {:start (start-point source target)
                                 :end (:entity/position target)
                                 :duration 0.05
                                 :color [1 0 0 0.75]
                                 :thick? true})]
       ; TODO => make new context with end-point ... and check on point entity
       ; friendly fire ?!
       ; player maybe just direction possible ?!
       [:tx/effect ctx hit-effect]]
      [; TODO
       ; * clicking on far away monster
       ; * hitting ground in front of you ( there is another monster )
       ; * -> it doesn't get hit ! hmmm
       ; * either use 'MISS' or get enemy entities at end-point
       [:tx/audiovisual (end-point source target maxrange) :effects.target-entity/hit-ground-effect]]))

  (effect/render-info [_ {:keys [effect/source effect/target] :as ctx}]
    (draw-line ctx
               (start-point source target)
               (end-point   source target maxrange)
               (if (in-range? source target maxrange)
                 [1 0 0 0.5]
                 [1 1 0 0.5]))))
