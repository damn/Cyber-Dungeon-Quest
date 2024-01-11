(ns cdq.context.ui.hp-mana-bars
  (:require [gdl.context :refer [draw-text draw-image create-image get-sub-image ->actor]]
            [utils.core :refer [readable-number]]
            [data.val-max :refer [val-max-ratio]]))

(defn- render-infostr-on-bar [{:keys [gui-viewport-width] :as c} infostr y h]
  (draw-text c
             {:text infostr
              :x (/ gui-viewport-width 2)
              :y (+ y 2)
              :up? true}))

(defn ->hp-mana-bars [context]
  (let [rahmen (create-image context "ui/rahmen.png")
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
    (->actor context
             {:draw (fn [{:keys [gui-viewport-width context/player-entity] :as c}]
                      (let [x (- (/ gui-viewport-width 2)
                                 (/ rahmenw 2))
                            y-hp 5
                            y-mana (+ y-hp rahmenh)]
                        (render-hpmana-bar c x y-hp   hpcontent   (:entity/hp  @player-entity) "HP")
                        (render-hpmana-bar c x y-mana manacontent (:entity/mana @player-entity) "MP")))})))
