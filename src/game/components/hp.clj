(ns game.components.hp
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.color :as color]
            [gdl.graphics.world :as world]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [data.val-max :refer [val-max-ratio]]
            [game.db :as db]
            [game.render :as render]
            [game.ui.config :refer (hpbar-height-px)]))

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
  (db/create [[_ max-hp]]
    [max-hp max-hp])
  (render/info [_ {:keys [body mouseover?]} [x y]]
    (let [{:keys [width half-width half-height]} body
          ratio (val-max-ratio hp)]
      (when (or (< ratio 1) mouseover?)
        (let [x (- x half-width)
              y (+ y half-height)
              height (world/pixels->world-units hpbar-height-px)
              border (world/pixels->world-units borders-px)]
          (shape-drawer/filled-rectangle x y width height color/black)
          (shape-drawer/filled-rectangle (+ x border)
                                         (+ y border)
                                         (- (* width ratio) (* 2 border))
                                         (- height (* 2 border))
                                         (hpbar-color ratio)))))))

(defn dead? [{:keys [hp]}]
  (zero? (hp 0)))

(defn- regenerate [val-max delta percent-reg-per-second]
  #_(apply-val val-max
               (->
                percent-reg-per-second
                (/ 100)         ; percent -> multiplier
                ; TODO no :max, its vector now
                (* (:max data)) ; in 1 second
                (/ 1000)        ; in 1 ms
                (* delta))))

#_(defcomponent :hp-regen
  (tick [_ {{:keys [reg-per-second]} :hp-regen :as entity*} delta]
    (update-in entity* [:hp] regenerate delta reg-per-second)))

(defn hp-regen-component [percent-reg-per-second]
  {:hp-regen
   {:reg-per-second percent-reg-per-second}})

#_(defcomponent :mana-regen
  (tick [_ {{:keys [reg-per-second]} :mana-regen :as entity*} delta]
    (update entity* :mana regenerate delta reg-per-second)))

(defn mana-regen-component [percent-reg-per-second]
  {:mana-regen
   {:reg-per-second percent-reg-per-second}})
