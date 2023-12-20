(ns game.effects.projectile
  (:require [clojure.string :as str]
            [gdl.math.vector :as v]
            [gdl.graphics.animation :as animation]
            [gdl.context :refer [get-sprite spritesheet]]
            [game.context :refer [projectile-entity path-blocked?]]
            [game.effect :as effect]))

; -> range needs to be smaller than potential field range
; -> first range check then ray ! otherwise somewhere in contentfield out of sight

(def ^:private size 0.5)
(def ^:private maxrange 10)
(def ^:private speed 10)
(def ^:private maxtime (/ maxrange (/ speed 1000)))

; TODO valid params direction has to be  non-nil (entities not los player ) ?
(defmethod effect/ai-should-use? :projectile [_ effect-params context entity*]
  (let [target (:target effect-params)]
    (and (not (path-blocked? context
                             (:position entity*) ; TODO test
                             (:position @target)
                             size))
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
  (projectile-entity context
                     {:position (:position @source)
                      :faction  (:faction  @source)
                      :size size
                      :animation (black-projectile context)
                      :speed speed
                      :movement-vector direction
                      :maxtime maxtime
                      :piercing false
                      :hit-effects hit-effects}))

(effect/defeffect :projectile
  {:text (fn [_effect-val params context]
           (str/join "\n"
                     (for [effect hit-effects] ; TODO make fn in effect/ for multiple text?
                       (effect/text effect params context))))
   ; TODO source,direction
   :valid-params? (fn [_effect-val {:keys [target]} _context]
                    target)
   :do! do-effect!})
