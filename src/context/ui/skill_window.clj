(ns context.ui.skill-window
  (:require [gdl.context :refer [->window ->image-button]]
            [gdl.scene2d.actor :refer [add-tooltip!]]
            [cdq.context :refer [get-property add-skill! skill-text]]
            [cdq.entity :as entity]))

(defn- pressed-on-skill-in-menu [{:keys [context/player-entity]
                                  :as context}
                                 skill]
  (when (and (pos? (:free-skill-points @player-entity))
             (not (entity/has-skill? @player-entity skill)))
    (swap! player-entity update :free-skill-points dec)
    (add-skill! context player-entity skill)))

; TODO render text label free-skill-points
; (str "Free points: " (:free-skill-points @player-entity))
(defn create [context]
  (->window context
            {:title "Skills"
             :id :skill-window
             :visible? false
             :rows [(for [id [:spells/projectile
                              :spells/meditation
                              :spells/spawn]
                          :let [skill (get-property context id)
                                button (->image-button context
                                                       (:property/image skill)
                                                       (fn [{:keys [context/player-entity] :as ctx}]
                                                         (when (= :idle (entity/state @player-entity))
                                                           (pressed-on-skill-in-menu ctx skill))))]]
                      ; duplicated @ action-bar => not skill-text but skill-button ... ? with different on-clicked
                      (do
                       (add-tooltip! button #(skill-text % skill))
                       button))]
             :pack? true}))
