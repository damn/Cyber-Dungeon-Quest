(ns context.ui.skill-window
  (:require [gdl.context :refer [->image-button]]
            [gdl.scene2d.ui :as ui]
            [app.state :refer [current-context]]
            [game.context :refer [get-property add-skill!]]
            [game.entity :as entity]
            [game.skill :as skill]))

(defn- pressed-on-skill-in-menu [{:keys [context/player-entity]
                                  :as context}
                                 skill]
  (when (and (pos? (:free-skill-points @player-entity))
             (not (entity/has-skill? @player-entity skill)))
    (swap! player-entity update :free-skill-points dec)
    (add-skill! context player-entity skill)))

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
