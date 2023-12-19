(ns game.effects.projectile
  (:require [clojure.string :as str]
            [gdl.math.vector :as v]
            [gdl.graphics.animation :as animation]
            [gdl.context :refer [get-sprite spritesheet]]
            [game.effect :as effect]
            [game.maps.cell-grid :as cell-grid]
            [game.entities.projectile :as projectile-entity]))

; -> range needs to be smaller than potential field range
; -> first range check then ray ! otherwise somewhere in contentfield out of sight

(def ^:private size 0.5)
(def ^:private maxrange 10)
(def ^:private speed 10)
(def ^:private maxtime (/ maxrange (/ speed 1000)))

; => world-map protocol ?
(defn- projectile-path-blocked? [{:keys [context/world-map]} start target]
  (cell-grid/is-path-blocked? world-map start target size))

; TODO valid params direction has to be  non-nil (entities not los player ) ?

(defmethod effect/ai-should-use? :projectile [_ effect-params context entity*]
  (let [target (:target effect-params)]
    (and (not (projectile-path-blocked? context
                                        (:position entity*) ; TODO test
                                        (:position @target)))
         ; TODO not taking into account body sizes
         (< (v/distance (:position entity*)
                        (:position @target))
            maxrange))))

(comment
 ; for chance to do hit-effect -> use this code @ hit-effects
 ; (extend hit-effects with chance , not effects themself)
 ; and hit-effects to text ...hmmm
 [utils.random :as random]
 (or (not chance)
     (random/percent-chance chance)))

(def ^:private hit-effects
  [[:damage [:magic [4 8]]]
   [:stun 500]
   ;[:stun {:duration 200} {:chance 100}]
   ])

(defn- black-projectile [context]
  (animation/create [(get-sprite context
                                 (spritesheet context "fx/uf_FX.png" 24 24)
                                 [1 12])]
                    :frame-duration 500))

(defn- do-effect! [_ {:keys [source direction]} context]
  (projectile-entity/create!
   {:position (:position @source)
    :faction  (:faction  @source)
    :size size
    :animation (black-projectile context)
    :speed speed
    :movement-vector direction
    :maxtime maxtime
    :piercing false
    :hit-effects hit-effects}
   context))

(effect/defeffect :projectile
  {:text (fn [_ params]
           (str/join "\n"
                     (for [effect hit-effects] ; TODO make fn in effect/ for multiple text?
                       (effect/text effect params))))
   ; TODO source,direction
   :valid-params? (fn [_ {:keys [target]}]
                    target)
   :do! do-effect!})
