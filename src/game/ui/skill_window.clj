(ns game.ui.skill-window
  (:require [gdl.context :refer [->image-button]]
            [gdl.scene2d.ui :as ui]
            [app.state :refer [current-context]]
            [game.context :refer [get-property ]]
            [game.skill :as skill]
            [game.components.skills :as skills]))

(defn- pressed-on-skill-in-menu [{:keys [context/player-entity]} skill]
  (when (and (pos? (:free-skill-points @player-entity))
             (not (skills/has-skill? (:skills @player-entity) skill)))
    (swap! player-entity #(-> %
                              (update :free-skill-points dec)
                              (update :skills skills/add-skill skill)))))

(defn create [context]
  (let [window (ui/window :title "Skills"
                          :id :skill-window)]
    (doseq [id [:spells/projectile
                :spells/meditation
                :spells/spawn]
            :let [skill (get-property context id)
                  button (->image-button context
                                         (:image skill)
                                         #(pressed-on-skill-in-menu % skill))]]
      (.addListener button (ui/text-tooltip (fn []
                                              (let [context @current-context]
                                                (skill/text skill
                                                            (:context/player-entity context)
                                                            context)))))
      (.add window button))
    ; TODO render text label free-skill-points
    ; (str "Free points: " (:free-skill-points @player-entity))
    (.pack window)
    window))
