(ns screens.game
  (:require [gdl.context :refer [render-gui-view render-world-view delta-time draw-text]]
            [gdl.screen :refer [Screen]]
            [gdl.maps.tiled :as tiled]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [app.state :refer [current-context]]
            [game.context :refer [render-entities* ray-blocked? explored? set-explored! get-active-entities line-of-sight?]]
            [game.tick :refer [tick-game]]
            game.ui.actors
            [game.ui.hp-mana-bars :refer [render-player-hp-mana]]
            [game.render.debug :as debug])
  (:import com.badlogic.gdx.graphics.Color))

(def ^:private explored-tile-color (Color. (float 0.5)
                                           (float 0.5)
                                           (float 0.5)
                                           (float 1)))

; TODO performance - need to deref current-context at every tile corner !!
; => see with prformance check later
; => need to pass to orthogonaltiledmap bla
; or pass only necessary data structures  (explored grid)
;=> the rays are more of a problem after sampling visualvm
(defn- tile-color-setter [_ x y]
  (let [{:keys [world-camera] :as context} @current-context
        light-position (camera/position world-camera)
        position [x y]
        explored? (explored? context position)
        base-color (if explored?
                     explored-tile-color
                     color/black)
        blocked? (ray-blocked? context light-position position)]
    (if blocked?
      base-color
      (do
       (when-not explored?
         (set-explored! context position))
       color/white))))

(defrecord SubScreen []
  Screen
  (show [_ _context])
  (hide [_ _context])
  (render [_ {:keys [context/world-map
                     context/player-entity]
              :as context}]
    (tiled/render-map context
                      (:tiled-map world-map)
                      tile-color-setter)
    (let [active-entities (get-active-entities context)] ; TODO call on content-grid ?
      (render-world-view context
                         (fn [context]
                           (debug/render-before-entities context)
                           (render-entities* context
                                             (->> active-entities
                                                  (map deref)
                                                  (filter #(line-of-sight? context @player-entity %))))
                           (debug/render-after-entities context)))
      (render-gui-view context
                       render-player-hp-mana)
      (tick-game context
                 active-entities
                 (* (delta-time context) 1000))))) ; TODO make in seconds ? no need to multiply by 1000 ?

(defn screen [context]
  {:actors (game.ui.actors/->ui-actors context)
   :sub-screen (screens.game/->SubScreen)})
