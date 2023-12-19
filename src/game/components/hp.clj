(ns game.components.hp
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.color :as color]
            [gdl.context :refer [draw-filled-rectangle pixels->world-units]]
            [data.val-max :refer [val-max-ratio]]
            [context.ecs :as entity]
            [game.ui.config :refer (hpbar-height-px)])
  (:import com.badlogic.gdx.graphics.Color))

(def ^:private hpbar-colors
  {:green     (color/rgb 0 0.8 0)
   :darkgreen (color/rgb 0 0.5 0)
   :yellow    (color/rgb 0.5 0.5 0)
   :red       (color/rgb 0.5 0 0)})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
                (> ratio 0.75) :green
                (> ratio 0.5)  :darkgreen
                (> ratio 0.25) :yellow
                :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defcomponent :hp hp
  (entity/create [[_ max-hp]]
    [max-hp max-hp])
  (entity/render-info [_ c {[x y] :position
                            {:keys [width half-width half-height]} :body
                            :keys [mouseover?]}]
    (let [ratio (val-max-ratio hp)]
      (when (or (< ratio 1) mouseover?)
        (let [x (- x half-width)
              y (+ y half-height)
              height (pixels->world-units c hpbar-height-px) ; pre-calculate it maybe somehow, but will put too much stuff in properties?
              border (pixels->world-units c borders-px)] ; => can actually still use global state? idk
          (draw-filled-rectangle c x y width height Color/BLACK)
          (draw-filled-rectangle c
                                 (+ x border)
                                 (+ y border)
                                 (- (* width ratio) (* 2 border))
                                 (- height (* 2 border))
                                 (hpbar-color ratio)))))))
