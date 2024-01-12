(ns cdq.entity.hp
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.color :as color]
            [gdl.context :refer [draw-filled-rectangle pixels->world-units]]
            [data.val-max :refer [val-max-ratio]]
            [cdq.entity :as entity]
            [cdq.context.ui.config :refer (hpbar-height-px)]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
                (> ratio 0.75) :green
                (> ratio 0.5)  :darkgreen
                (> ratio 0.25) :yellow
                :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defcomponent :entity/hp hp
  (entity/create-component [[_ max-hp] _components _ctx]
    [max-hp max-hp])

  (entity/render-info [_ {[x y] :entity/position
                          {:keys [width half-width half-height]} :entity/body
                          :keys [entity/mouseover?]}
                       c]
    (let [ratio (val-max-ratio hp)]
      (when (or (< ratio 1) mouseover?)
        (let [x (- x half-width)
              y (+ y half-height)
              height (pixels->world-units c hpbar-height-px) ; pre-calculate it maybe somehow, but will put too much stuff in properties?
              border (pixels->world-units c borders-px)] ; => can actually still use global state? idk
          (draw-filled-rectangle c x y width height color/black)
          (draw-filled-rectangle c
                                 (+ x border)
                                 (+ y border)
                                 (- (* width ratio) (* 2 border))
                                 (- height (* 2 border))
                                 (hpbar-color ratio)))))))
