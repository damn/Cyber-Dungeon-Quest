(ns game.ui.skill-window
  (:require clojure.set
            [gdl.scene2d.ui :as ui]
            [utils.core :refer [safe-get]]
            [game.components.skills :refer (assoc-skill has-skill?)]
            [game.skills.core :as skills]
            [game.player.entity :refer (player-entity)]))

(def ^:privat skill-icon-size 48)

; TODO this is actually two modifiers ... skill and free-skill-p
; and can do modifier-text @ tooltip ....
; 'skills' menu is a modifier menu ...
; you can have more general modifiers menus toggling on/off
; passives toggle-able ... wtf / stances ...
(defn- pressed-on-skill-in-menu [skill-id]
  (when (and (pos? (:free-skill-points @player-entity))
             (not (has-skill? @player-entity skill-id)))
    (swap! player-entity #(-> %
                              (update :free-skill-points dec)
                              (update :skills assoc-skill skill-id)))))

(defn create [{:keys [context/properties]}]
  (let [window (ui/window :title "Skills"
                          :id :skill-window)]
    (doseq [id [:projectile
                :meditation
                :spawn]
            :let [skill (safe-get properties id)
                  button (ui/image-button (:image skill)
                                          #(pressed-on-skill-in-menu id))]]
      (.addListener button (ui/text-tooltip #(skills/text id player-entity)))
      (.add window button))
    ; TODO render text label free-skill-points
    ; (str "Free points: " (:free-skill-points @player-entity))
    (.pack window)
    window))
