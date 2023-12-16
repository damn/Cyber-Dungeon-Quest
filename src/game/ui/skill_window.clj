(ns game.ui.skill-window
  (:require clojure.set
            [gdl.app :as app]
            [gdl.scene2d.ui :as ui]
            [utils.core :refer [safe-get]]
            [game.skill :as skill]
            [game.components.skills :as skills]))

(def ^:privat skill-icon-size 48)

; TODO this is actually two modifiers ... skill and free-skill-p
; and can do modifier-text @ tooltip ....
; 'skills' menu is a modifier menu ...
; you can have more general modifiers menus toggling on/off
; passives toggle-able ... wtf / stances ...
(defn- pressed-on-skill-in-menu [skill]
  (let [{:keys [context/player-entity]} @app/state]
    (when (and (pos? (:free-skill-points @player-entity))
               (not (skills/has-skill? (:skills @player-entity) skill)))
      (swap! player-entity #(-> %
                                (update :free-skill-points dec)
                                (update :skills skills/add-skill skill))))))

(defn create [{:keys [context/properties]}]
  (let [window (ui/window :title "Skills"
                          :id :skill-window)]
    (doseq [id [:projectile
                :meditation
                :spawn]
            :let [skill (safe-get properties id)
                  button (ui/image-button (:image skill)
                                          #(pressed-on-skill-in-menu skill))]]
      (.addListener button (ui/text-tooltip (fn []
                                              ; player-entity is not yet added to context
                                              (skill/text skill (:context/player-entity @app/state)))))
      (.add window button))
    ; TODO render text label free-skill-points
    ; (str "Free points: " (:free-skill-points @player-entity))
    (.pack window)
    window))
