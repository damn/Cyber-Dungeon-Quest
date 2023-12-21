(ns screens.game
  (:require [gdl.context :refer [render-gui-view render-world-view delta-time]]
            [gdl.screen :refer [Screen]]
            [gdl.maps.tiled :as tiled]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [app.state :refer [current-context]]
            [game.context :refer [render-visible-entities ray-blocked? explored? set-explored!]]
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
  (render [_ {:keys [context/world-map] :as context}]
    (tiled/render-map context
                      (:tiled-map world-map)
                      tile-color-setter)
    (render-world-view context
                       (fn [c]
                         (debug/render-before-entities c)
                         (render-visible-entities      c)
                         (debug/render-after-entities  c)))
    (render-gui-view context render-player-hp-mana)
    (tick-game context (* (delta-time context) 1000))))

(defn screen [context]
  {:actors (game.ui.actors/create-actors context)
   :sub-screen (screens.game/->SubScreen)})
