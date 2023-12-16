(ns game.ui.mouseover-entity
  (:require [gdl.scene2d.stage :as stage]
            [utils.core :refer [sort-by-order]]
            [game.context :as gm]
            game.render
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.cell-grid :refer (get-bodies-at-position)])
  (:import (com.badlogic.gdx Gdx Input$Buttons)))


(defn- get-current-mouseover-entity [{:keys [world-mouse-position
                                             context/player-entity
                                             context/world-map]
                                      :as context}]
  (let [cell-grid (:cell-grid world-map)
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

(defn reset-cache! []
  (reset! cache nil)
  (reset! is-saved false))

(defn get-mouseover-entity []
  @cache)

(defn saved-mouseover-entity
  "When the player keeps holding leftmouse down after having a mouseoverbody it is saved."
  []
  (and @is-saved @cache))

(defn- keep-saved? [entity {:keys [context/player-entity] :as context}]
  (and (.isButtonPressed Gdx/input Input$Buttons/LEFT)
       (gm/entity-exists? context entity)
       (in-line-of-sight? @player-entity @entity context)))

(defn- save? [{:keys [context/player-entity]} entity]
  (and (.isButtonJustPressed Gdx/input Input$Buttons/LEFT) ; dont move & get stuck on any entity under mouse, only save when starting to click on that
       (.isButtonPressed     Gdx/input Input$Buttons/LEFT)
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
       (reset! is-saved (save? context entity))
       (swap! entity assoc :mouseover? true))
      (do
       (reset! cache nil)
       (reset! is-saved false)))))
