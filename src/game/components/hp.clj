(ns game.components.hp
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.color :as color]
            [gdl.graphics.world :as world]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [utils.core :refer :all]
            [game.db :as db]
            [game.render :as render]
            [game.modifier :as modifier]
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
    (val-max max-hp))
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

(modifier/defmodifier :hp
  {:values  [[15 25] [35 45] [55 65]]
   :text    #(str "+" % " HP")
   :keys    [:hp]
   :apply   (partial apply-max +)
   :reverse (partial apply-max -)})

;; Regeneration

(defn- regenerate [val-max delta percent-reg-per-second]
  #_(apply-val val-max
               (->
                percent-reg-per-second
                (/ 100)         ; percent -> multiplier
                ; TODO no :max, its vector now
                (* (:max data)) ; in 1 second
                (/ 1000)        ; in 1 ms
                (* delta))))

; TODO move into skillmanager / destructible
; hp/mana regen value (default 0)

; TODO check destructible/hp is there !

#_(defcomponent :hp-regen
  (tick [_ {{:keys [reg-per-second]} :hp-regen :as entity*} delta]
    (update-in entity* [:hp] regenerate delta reg-per-second)))

; hp-regeneration can even be a separate component which regenerates hp (assert HP is there )
; or doesnt even need to be there ? a door with high HP regen but no hp
; -> make alive -> regens it fast

; TODO move to hitpoints ns
(defn hp-regen-component [percent-reg-per-second]
  {:hp-regen
   {:reg-per-second percent-reg-per-second}})

; TODO entities with mana but no skillmanager ?
; -> why not ? butterflys who have mana which you can steal or take when died
; -> but they cannot cast skills
; -> how about when an entity dies you can collect its mana ? no mana potions ?
; it disappears after a while
; and hitpoints ? you can steal life also ?
; -> no mana/hit potions and verteilung needed ?
; -> you can get more if you one-hit monsters, (full HP)
; if you deal slowly damage you dont get any hitpoints ? weird...
; -> with autopickup (also gold and items ? unlimited inventory ? 'weight')

; TODO check :skillmanager / :mana is there !

#_(defcomponent :mana-regen
  (tick [_ {{:keys [reg-per-second]} :mana-regen :as entity*} delta]
    (update entity* :mana regenerate delta reg-per-second)))

; hp-regeneration can even be a separate component which regenerates hp (assert HP is there )
; or doesnt even need to be there ? a door with high HP regen but no hp
; -> make alive -> regens it fast

(defn mana-regen-component [percent-reg-per-second]
  {:mana-regen
   {:reg-per-second percent-reg-per-second}})

(modifier/defmodifier :hp-reg
  {:values  [[5 15] [16 25] [26 35]]
   :text    #(str "+" (readable-number (/ % 10)) "% HP-Reg-per-second")
   :keys    [:hp-regen :reg-per-second] ; -> just :hp-reg / :mana-reg ?
   :apply   #(+ %1 (/ %2 10))
   :reverse #(- %1 (/ %2 10))})

(modifier/defmodifier :mana-reg
  {:values  [[10 30] [31 50] [51 75]]
   :text    #(str "+" (readable-number (/ % 10)) "% MP-Reg-per-second")
   :keys    [:mana-regen :reg-per-second]
   :apply   #(+ %1 (/ %2 10))
   :reverse #(- %1 (/ %2 10))})
