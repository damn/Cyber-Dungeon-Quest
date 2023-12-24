(ns context.ui.skill-window
  (:require [gdl.context :refer [->window ->image-button ->text-tooltip]]
            [cdq.context :refer [get-property add-skill! skill-text]]
            [cdq.entity :as entity]))

(defn- pressed-on-skill-in-menu [{:keys [context/player-entity]
                                  :as context}
                                 skill]
  (when (and (pos? (:free-skill-points @player-entity))
             (not (entity/has-skill? @player-entity skill)))
    (swap! player-entity update :free-skill-points dec)
    (add-skill! context player-entity skill)))

(defn create [context]
  (let [window (->window context
                         {:title "Skills"
                          :id :skill-window})]
    (doseq [id [:spells/projectile
                :spells/meditation
                :spells/spawn]
            :let [skill (get-property context id)
                  button (->image-button context
                                         (:image skill)
                                         #(pressed-on-skill-in-menu % skill))]]
      ; duplicated @ action-bar
      (.addListener button (->text-tooltip context #(skill-text % skill)))
      (.add window button))
    ; TODO render text label free-skill-points
    ; (str "Free points: " (:free-skill-points @player-entity))
    (.pack window)
    window))
