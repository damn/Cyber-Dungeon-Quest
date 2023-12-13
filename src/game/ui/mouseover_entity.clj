(ns game.ui.mouseover-entity
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.color :as color]
            [gdl.draw :as draw]
            [gdl.scene2d.stage :as stage]
            [utils.core :refer [sort-by-order]]
            [game.db :as db]
            [game.entity :as entity]
            game.render
            [game.session :as session]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.data :refer (get-current-map-data)]
            [game.maps.cell-grid :refer (get-bodies-at-position)]
            [game.player.entity :refer (player-entity)])
  (:import (com.badlogic.gdx Gdx Input$Buttons)))

(def ^:private outline-alpha 0.4)
(color/defrgb ^:private enemy-color    1 0 0 outline-alpha)
(color/defrgb ^:private friendly-color 0 1 0 outline-alpha)
(color/defrgb ^:private neutral-color  1 1 1 outline-alpha)

(defcomponent :mouseover? _
  (entity/render-below [_ drawer _ctx {:keys [position body faction]}]
    (draw/with-line-width drawer 3
      #(draw/ellipse drawer position
                     (:half-width body)
                     (:half-height body)
                     (case faction ; TODO enemy faction of player
                       :evil friendly-color
                       :good enemy-color
                       neutral-color)))))

(defn- get-current-mouseover-entity [{:keys [world-mouse-position] :as context}]
  (let [cell-grid (:cell-grid (get-current-map-data))
        hits (get-bodies-at-position cell-grid world-mouse-position)]
    ; TODO needs z-order ? what if 'shout' element or FX ?
    (when hits
      (->> game.render/render-on-map-order
           ; TODO re-use render-ingame code to-be-rendered-entities-on-map
           (sort-by-order hits #(:z-order @%))
           ; topmost body selected first, reverse of render-order
           reverse
           ; = same code @ which entities should get rendered...
           (filter #(in-line-of-sight? @player-entity @% context))
           first))))

(def ^:private cache (atom nil))
(def ^:private is-saved (atom false))

(def state (reify session/State
             (load! [_ data]
               (reset! cache nil)
               (reset! is-saved false))
             (serialize [_])
             (initial-data [_])))

(defn get-mouseover-entity []
  @cache)

(defn saved-mouseover-entity
  "When the player keeps holding leftmouse down after having a mouseoverbody it is saved."
  []
  (and @is-saved @cache))

(defn- keep-saved? [entity context]
  (and (.isButtonPressed Gdx/input Input$Buttons/LEFT)
       (db/exists? entity)
       (in-line-of-sight? @player-entity @entity context)))

(defn- save? [entity]
  (and (.isButtonJustPressed Gdx/input Input$Buttons/LEFT) ; dont move & get stuck on any entity under mouse, only save when starting to click on that
       (.isButtonPressed Gdx/input Input$Buttons/LEFT)
       (not= entity player-entity))) ; movement follows this / targeting / ...

(defn update-mouseover-entity [stage {:keys [gui-mouse-position] :as context}]
  (when-not (and @is-saved
                 (keep-saved? @cache context))
    (when-let [entity @cache]
      (swap! entity dissoc :mouseover?))
    (if-let [entity (when-not (stage/hit stage gui-mouse-position)
                      (get-current-mouseover-entity context))]
      (do
       (reset! cache entity)
       (reset! is-saved (save? entity))
       (swap! entity assoc :mouseover? true))
      (do
       (reset! cache nil)
       (reset! is-saved false)))))
