(ns game.components.hp
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.color :as color]
            [gdl.graphics.world :as world]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [data.val-max :refer [val-max-ratio]]
            [game.entity :as entity]
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
  (entity/render-info [_ context {[x y] :position :keys [body mouseover?]}]
    (let [{:keys [width half-width half-height]} body
          ratio (val-max-ratio hp)]
      (when (or (< ratio 1) mouseover?)
        (let [x (- x half-width)
              y (+ y half-height)
              height (world/pixels->world-units hpbar-height-px)
              border (world/pixels->world-units borders-px)]
          (shape-drawer/filled-rectangle x y width height Color/BLACK)
          (shape-drawer/filled-rectangle (+ x border)
                                         (+ y border)
                                         (- (* width ratio) (* 2 border))
                                         (- height (* 2 border))
                                         (hpbar-color ratio)))))))

(defn dead? [{:keys [hp]}]
  (zero? (hp 0)))
