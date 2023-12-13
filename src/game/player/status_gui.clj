(ns game.player.status-gui
  (:require [x.x :refer [defmodule]]
            [gdl.draw :as draw]
            [gdl.lifecycle :as lc]
            [gdl.graphics.image :as image]
            [utils.core :refer [readable-number]]
            [data.val-max :refer [val-max-ratio]]
            [game.player.entity :refer (player-entity)]))

(defn- render-infostr-on-bar [drawer {:keys [gui-viewport-width]} infostr y h]
  (draw/text drawer
             {:text infostr
              :x (/ gui-viewport-width 2)
              :y (+ y 2)
              :up? true}))

(defmodule _
  (lc/create [_ context]
    (let [scale 2] ; TODO FIXME scale of the whole all game things can set somewhere (gui-scale) for all dists, etc.
      ; ?? can play also sci-fi 24x24 ?
      (def ^:private rahmen (image/create context "ui/rahmen.png"))
      (def ^:private rahmenw (first  (:pixel-dimensions rahmen)))
      (def ^:private rahmenh (second (:pixel-dimensions rahmen)))
      (def ^:private hpcontent   (image/create context "ui/hp.png"))
      (def ^:private manacontent (image/create context "ui/mana.png")))))

(defn- render-hpmana-bar [drawer context x y contentimg minmaxval name]
  ; stack
  ; * rahmen
  ; * sub-image
  ; * label
  (draw/image drawer rahmen x y)
  (draw/image drawer (image/get-sub-image context contentimg 0 0
                                          (* rahmenw (val-max-ratio minmaxval)) rahmenh) x y)
  (render-infostr-on-bar drawer context (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) y rahmenh))

(defn- render-player-stats [drawer {:keys [gui-viewport-width] :as context}]
  (let [x (- (/ gui-viewport-width 2)
             (/ rahmenw 2))
        y-hp 54
        y-mana (+ y-hp rahmenh)]
    (render-hpmana-bar drawer context x y-hp   hpcontent   (:hp   @player-entity) "HP")
    (render-hpmana-bar drawer context x y-mana manacontent (:mana @player-entity) "MP")))

(defn render-player-hp-mana [drawer context]
  (render-player-stats drawer context))
