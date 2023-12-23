(ns context.ui.hp-mana-bars
  (:require [gdl.app :refer [current-context]]
            [gdl.context :refer [draw-text draw-image create-image get-sub-image]]
            [utils.core :refer [readable-number]]
            [data.val-max :refer [val-max-ratio]])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- render-infostr-on-bar [{:keys [gui-viewport-width] :as c} infostr y h]
  (draw-text c
             {:text infostr
              :x (/ gui-viewport-width 2)
              :y (+ y 2)
              :up? true}))

(defn ->hp-mana-bars [context]
  (let [scale 2
        rahmen (create-image context "ui/rahmen.png")
        rahmenw (first  (:pixel-dimensions rahmen))
        rahmenh (second (:pixel-dimensions rahmen))
        hpcontent   (create-image context "ui/hp.png")
        manacontent (create-image context "ui/mana.png")
        render-hpmana-bar (fn [ctx x y contentimg minmaxval name]
                            (draw-image ctx rahmen [x y])
                            (draw-image ctx
                                        (get-sub-image ctx (assoc contentimg :sub-image-bounds [0 0 (* rahmenw (val-max-ratio minmaxval)) rahmenh]))
                                        [x y])
                            (render-infostr-on-bar ctx (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) y rahmenh))]
    (proxy [Actor] []
      (draw [_batch _parent-alpha]
        (let [{:keys [gui-viewport-width context/player-entity] :as c} @current-context
              x (- (/ gui-viewport-width 2)
                   (/ rahmenw 2))
              y-hp 54
              y-mana (+ y-hp rahmenh)]
          (render-hpmana-bar c x y-hp   hpcontent   (:hp   @player-entity) "HP")
          (render-hpmana-bar c x y-mana manacontent (:mana @player-entity) "MP"))))))
