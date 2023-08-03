(nsx game.player.status-gui
  (:require [game.player.entity :refer (player-entity)])
  (:use
    (game media)
    [game.player.session-data :only (current-character-name)]))

(defn- render-infostr-on-bar [infostr y h]
  (g/draw-text {:font media/font
                :text infostr
                :x (/ (g/viewport-width) 2)
                :y (+ y 2)
                :up? true}))

(app/on-create
 (let [scale 2] ; TODO FIXME scale of the whole all game things can set somewhere (gui-scale) for all dists, etc.
                ; ?? can play also sci-fi 24x24 ?
   (def- rahmen (image/create "ui/rahmen.png"))
   (def- rahmenw (first  (image/pixel-dimensions rahmen)))
   (def- rahmenh (second (image/pixel-dimensions rahmen)))
   (def- hpcontent   (image/create "ui/hp.png"))
   (def- manacontent (image/create "ui/mana.png"))))

(defn- render-hpmana-bar [x y contentimg minmaxval name]
  (image/draw rahmen x y)
  (image/draw (image/get-sub-image contentimg 0 0 (* rahmenw (val-max-ratio minmaxval)) rahmenh) x y)
  (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) y rahmenh))

(defn- render-player-stats []
  (let [x (- (/ (g/viewport-width) 2)
             (/ rahmenw 2))
        y-hp 54
        y-mana (+ y-hp rahmenh)]
    (render-hpmana-bar x y-hp   hpcontent   (:hp   @player-entity) "HP")
    (render-hpmana-bar x y-mana manacontent (:mana @player-entity) "MP")))

(defn render-player-hp-mana []
  (render-player-stats))
