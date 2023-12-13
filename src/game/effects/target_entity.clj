(ns game.effects.target-entity ; TODO naming
  (:require [clojure.string :as str]
            [gdl.app :as app]
            [gdl.vector :as v]
            [gdl.graphics.color :as color]
            [gdl.draw :as draw]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.components.skills :refer (ai-should-use?)]
            [game.entities.audiovisual :as audiovisual]
            [game.entities.line :as line-entity]
            [game.effect :as effect]))

; TODO target still exists ?! necessary ? what if disappears/dead?
(defn- valid-params? [_ {:keys [source target]}]
  (and source
       target
       (in-line-of-sight? @source @target (app/current-context)) ; TODO move to valid-params? @ extra param... !!
       (:hp @target))) ; TODO this is valid-params of hit-effect damage !!

(defn- in-range? [entity* target* maxrange] ; == circle-collides?
  (< (- (v/distance (:position entity*)
                    (:position target*))
        (:radius (:body entity*))
        (:radius (:body target*)))
     maxrange))

; TODO pass effect-params here.
(defmethod ai-should-use? :target-entity [[effect-id effect-value] entity*]
  (in-range? entity*
             @(:target (:effect-params (:skillmanager entity*)))
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
     (audiovisual/create! context
                          (end-point @source @target maxrange)
                          :effects.target-entity/hit-ground-effect))))

(defmethod effect/render-info :target-entity [drawer [_ {:keys [maxrange]}] {:keys [source target]}]
  (draw/line drawer
             (start-point @source @target)
             (end-point   @source @target maxrange)
             (if (in-range? @source @target maxrange)
               (color/rgb 1 0 0 0.5)
               (color/rgb 1 1 0 0.5))))

(effect/defeffect :target-entity
  {:text (fn [{:keys [maxrange hit-effects]} params]
           (str "Range " maxrange " meters\n"
                (str/join "\n" ; TODO same as other effect multiple text -> effect/effects-text?
                          (for [effect hit-effects]
                            (effect/text effect params)))))
   :valid-params? valid-params?
   :do! do-effect!})
