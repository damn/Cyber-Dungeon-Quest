(ns game.player.status-gui
  (:require [x.x :refer [defmodule]]
            gdl.app
            [gdl.lc :as lc]
            [gdl.graphics.font :as font]
            [gdl.graphics.image :as image]
            [gdl.graphics.gui :as gui]
            [utils.core :refer [readable-number]]
            [data.val-max :refer [val-max-ratio]]
            [game.player.entity :refer (player-entity)]))

(defn- render-infostr-on-bar [context infostr y h]
  (font/draw-text context
                  {:text infostr
                   :x (/ (gui/viewport-width) 2)
                   :y (+ y 2)
                   :up? true}))

(defmodule _
  (lc/create [_ context]
    (let [scale 2] ; TODO FIXME scale of the whole all game things can set somewhere (gui-scale) for all dists, etc.
      ; ?? can play also sci-fi 24x24 ?
      (def ^:private rahmen (image/create context "ui/rahmen.png"))
      (def ^:private rahmenw (first  (image/pixel-dimensions rahmen)))
      (def ^:private rahmenh (second (image/pixel-dimensions rahmen)))
      (def ^:private hpcontent   (image/create context "ui/hp.png"))
      (def ^:private manacontent (image/create context "ui/mana.png")))))

(defn- render-hpmana-bar [context x y contentimg minmaxval name]
  ; stack
  ; * rahmen
  ; * sub-image
  ; * label
  (image/draw context rahmen x y)
  (image/draw context (image/get-sub-image context contentimg 0 0
                                           (* rahmenw (val-max-ratio minmaxval)) rahmenh) x y)
  (render-infostr-on-bar context (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) y rahmenh))

(defn- render-player-stats [context]
  (let [x (- (/ (gui/viewport-width) 2)
             (/ rahmenw 2))
        y-hp 54
        y-mana (+ y-hp rahmenh)]
    (render-hpmana-bar context x y-hp   hpcontent   (:hp   @player-entity) "HP")
    (render-hpmana-bar context x y-mana manacontent (:mana @player-entity) "MP")))

(defn render-player-hp-mana [context]
  (render-player-stats context))
