(ns game.effects.target-entity
  (:require [gdl.context :refer [draw-line]]
            [gdl.math.vector :as v]
            [gdl.graphics.color :as color]
            [game.context :refer (do-effect! effect-text audiovisual line-entity line-of-sight?)]
            [game.effect :as effect]))

(defn- in-range? [entity* target* maxrange] ; == circle-collides?
  (< (- (v/distance (:position entity*)
                    (:position target*))
        (:radius (:body entity*))
        (:radius (:body target*)))
     maxrange))

(defmethod effect/useful? :target-entity
  [{:keys [target]} [_type value]]
  (in-range? entity* ;TODO  source ?
             @target
             (:maxrange value)))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity* target*]
  (v/add (:position entity*)
         (v/scale (v/direction (:position entity*)
                               (:position target*))
                  (:radius (:body entity*)))))

(defn- end-point [entity* target* maxrange]
  (v/add (start-point entity* target*)
         (v/scale (v/direction (:position entity*)
                               (:position target*))
                  maxrange)))

(defmethod effect/render-info :target-entity
  [{:keys [source target] :as context}
   [_ {:keys [maxrange]}]]
  (draw-line context
             (start-point @source @target)
             (end-point   @source @target maxrange)
             (if (in-range? @source @target maxrange)
               (color/rgb 1 0 0 0.5)
               (color/rgb 1 1 0 0.5))))

(defmethod effect/text :target-entity
  [context [_ {:keys [maxrange hit-effect]}]]
  (str "Range " maxrange " meters\n"
       ; TODO already merged before calling text? ... when are they coming from ?
       (effect-text (merge context params) hit-effect)))

; TODO target still exists ?! necessary ? what if disappears/dead?
; TODO this is valid-params of hit-effect damage !!
(defmethod effect/valid-params? :target-entity
  [{:keys [source target]} _effect]
  (and source
       target
       (line-of-sight? context @source @target)
       (:hp @target)))

(defmethod effect/do! :target-entity
  [{:keys [source target] :as context} [_ {:keys [hit-effect maxrange]}]]
  (if (in-range? @source @target maxrange)
    (do
     (line-entity context
                  {:start (start-point @source @target)
                   :end (:position @target)
                   :duration 50
                   :color (color/rgb 1 0 0 0.75)
                   :thick? true})
     (do-effect! (merge context
                        {:effect/source source :effect/target target})
                 hit-effect ))
    (do
     ; TODO
     ; * clicking on far away monster
     ; * hitting ground in front of you ( there is another monster )
     ; * -> it doesn't get hit ! hmmm
     ; * either use 'MISS' or get enemy entities at end-point
     (audiovisual context
                  (end-point @source @target maxrange)
                  :effects.target-entity/hit-ground-effect))))
