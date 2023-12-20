(ns game.effects.target-entity ; TODO naming
  (:require [clojure.string :as str]
            [gdl.context :refer [draw-line]]
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

(defmethod effect/useful? :target-entity [[effect-id effect-value] effect-params _context entity*]
  (in-range? entity*
             @(:target effect-params)
             (:maxrange effect-value)))

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

(defn- do-effect! [context {:keys [hit-effect maxrange]} {:keys [source target]}]
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
     ; * clicking on far away monster
     ; * hitting ground in front of you ( there is another monster )
     ; * -> it doesn't get hit ! hmmm
     ; * either use 'MISS' or get enemy entities at end-point
     (audiovisual context
                  (end-point @source @target maxrange)
                  :effects.target-entity/hit-ground-effect))))

(defmethod effect/render-info :target-entity [c [_ {:keys [maxrange]}] {:keys [source target]}]
  (draw-line c
             (start-point @source @target)
             (end-point   @source @target maxrange)
             (if (in-range? @source @target maxrange)
               (color/rgb 1 0 0 0.5)
               (color/rgb 1 1 0 0.5))))

(effect/component :target-entity
  {:text (fn [context {:keys [maxrange hit-effect]} params]
           (str "Range " maxrange " meters\n"
                ; TODO already merged before calling text? ... when are they coming from ?
                (effect-text (merge context params) hit-effect)))
   :valid-params?
   ; TODO target still exists ?! necessary ? what if disappears/dead?
   (fn [context _effect-val {:keys [source target]}]
     (and source
          target
          (line-of-sight? context @source @target)
          (:hp @target))) ; TODO this is valid-params of hit-effect damage !!

   :do! do-effect!})
