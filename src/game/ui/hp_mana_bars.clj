(ns game.ui.hp-mana-bars
  (:require [gdl.protocols :refer [draw-text draw-image create-image get-sub-image]]
            [utils.core :refer [readable-number]]
            [data.val-max :refer [val-max-ratio]]))

(defn- render-infostr-on-bar [{:keys [gui-viewport-width] :as c} infostr y h]
  (draw-text c
             {:text infostr
              :x (/ gui-viewport-width 2)
              :y (+ y 2)
              :up? true}))

(defn initialize! [context]
  (let [scale 2] ; TODO FIXME scale of the whole all game things can set somewhere (gui-scale) for all dists, etc.
    ; ?? can play also sci-fi 24x24 ?
    (def ^:private rahmen (create-image context "ui/rahmen.png"))
    (def ^:private rahmenw (first  (:pixel-dimensions rahmen)))
    (def ^:private rahmenh (second (:pixel-dimensions rahmen)))
    (def ^:private hpcontent   (create-image context "ui/hp.png"))
    (def ^:private manacontent (create-image context "ui/mana.png"))))

(defn- render-hpmana-bar [c x y contentimg minmaxval name]
  ; stack
  ; * rahmen
  ; * sub-image
  ; * label
  (draw-image c rahmen x y)
  (draw-image c (get-sub-image c
                               (assoc contentimg
                                      :sub-image-bounds [0 0 (* rahmenw (val-max-ratio minmaxval)) rahmenh]))
              x y)
  (render-infostr-on-bar c (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) y rahmenh))

(defn render-player-hp-mana [{:keys [gui-viewport-width context/player-entity] :as c}]
  (let [x (- (/ gui-viewport-width 2)
             (/ rahmenw 2))
        y-hp 54
        y-mana (+ y-hp rahmenh)]
    (render-hpmana-bar c x y-hp   hpcontent   (:hp   @player-entity) "HP")
    (render-hpmana-bar c x y-mana manacontent (:mana @player-entity) "MP")))
