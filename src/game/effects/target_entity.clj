(ns game.effects.target-entity ; TODO naming
  (:require [clojure.string :as str]
            [gdl.context :refer [draw-line]]
            [gdl.math.vector :as v]
            [gdl.graphics.color :as color]
            [game.context :refer (audiovisual in-line-of-sight?)]
            [game.entities.line :as line-entity]
            [game.effect :as effect]))

(defn- in-range? [entity* target* maxrange] ; == circle-collides?
  (< (- (v/distance (:position entity*)
                    (:position target*))
        (:radius (:body entity*))
        (:radius (:body target*)))
     maxrange))

(defmethod effect/ai-should-use? :target-entity [[effect-id effect-value] effect-params _context entity*]
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

(defn- do-effect! [{:keys [hit-effects maxrange]} {:keys [source target]} context]
  (if (in-range? @source @target maxrange)
    (do
     (line-entity/create! context
                          {:start (start-point @source @target)
                           :end (:position @target)
                           :duration 50
                           :color (color/rgb 1 0 0 0.75)
                           :thick? true})
     (effect/do-all! hit-effects {:source source :target target} context))
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

(effect/defeffect :target-entity
  {:text (fn [{:keys [maxrange hit-effects]} params context]
           (str "Range " maxrange " meters\n"
                (str/join "\n" ; TODO same as other effect multiple text -> effect/effects-text?
                          ; maybr work with 'effect' as a list of effect-components
                          ; so alsways 'effect' and not 'effects'
                          (for [effect hit-effects]
                            (effect/text effect params context)))))
   :valid-params?
   ; TODO target still exists ?! necessary ? what if disappears/dead?
   (fn [_effect-val {:keys [source target]} context]
     (and source
          target
          (in-line-of-sight? context @source @target)
          (:hp @target))) ; TODO this is valid-params of hit-effect damage !!

   :do! do-effect!})
