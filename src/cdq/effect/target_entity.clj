(ns cdq.effect.target-entity
  (:require [malli.core :as m]
            [gdl.context :refer [draw-line]]
            [gdl.math.vector :as v]
            [cdq.context :refer (effect-text line-of-sight? line-entity)]
            [cdq.effect :as effect]))

(def ^:private schema
  (m/schema [:map [:hit-effect [:map]] [:maxrange pos?]]))

(defmethod effect/value-schema :effect/target-entity [_]
  schema)

(defn- in-range? [entity* target* maxrange] ; == circle-collides?
  (< (- (v/distance (:entity/position entity*)
                    (:entity/position target*))
        (:radius (:entity/body entity*))
        (:radius (:entity/body target*)))
     maxrange))

(defmethod effect/useful? :effect/target-entity
  [{:keys [effect/source
           effect/target]}
   [_ {:keys [maxrange]}]]
  (in-range? @source @target maxrange))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity* target*]
  (v/add (:entity/position entity*)
         (v/scale (v/direction (:entity/position entity*)
                               (:entity/position target*))
                  (:radius (:entity/body entity*)))))

(defn- end-point [entity* target* maxrange]
  (v/add (start-point entity* target*)
         (v/scale (v/direction (:entity/position entity*)
                               (:entity/position target*))
                  maxrange)))

(defmethod effect/render-info :effect/target-entity
  [{:keys [effect/source
           effect/target] :as context}
   [_ {:keys [maxrange]}]]
  (draw-line context
             (start-point @source @target)
             (end-point   @source @target maxrange)
             (if (in-range? @source @target maxrange)
               [1 0 0 0.5]
               [1 1 0 0.5])))

(defmethod effect/text :effect/target-entity
  [context [_ {:keys [maxrange hit-effect]}]]
  (str "Range " maxrange " meters\n" (effect-text context hit-effect)))

; TODO target still exists ?! necessary ? what if disappears/dead?
; TODO this is valid-params of hit-effect damage !!
(defmethod effect/valid-params? :effect/target-entity
  [{:keys [effect/source
           effect/target] :as context}
   _effect]
  (and source
       target
       (line-of-sight? context @source @target)
       (:entity/hp @target)))

(defmethod effect/transactions :effect/target-entity
  [{:keys [effect/source
           effect/target] :as context}
   [_ {:keys [hit-effect maxrange]}]]
  (if (in-range? @source @target maxrange)
    [(line-entity context
                  {:start (start-point @source @target)
                   :end (:entity/position @target)
                   :duration 0.05
                   :color [1 0 0 0.75]
                   :thick? true})
     ; TODO => make new context with end-point ... and check on point entity
     ; friendly fire ?!
     ; player maybe just direction possible ?!
     [:tx/effect context hit-effect]]
    [; TODO
     ; * clicking on far away monster
     ; * hitting ground in front of you ( there is another monster )
     ; * -> it doesn't get hit ! hmmm
     ; * either use 'MISS' or get enemy entities at end-point
     [:tx/audiovisual (end-point @source @target maxrange) :effects.target-entity/hit-ground-effect]]))