(ns context.ui.skill-window
  (:require [gdl.context :refer [->window ->image-button]]
            [gdl.scene2d.actor :refer [add-tooltip!]]
            [cdq.context :refer [get-property add-skill! tooltip-text]]
            [cdq.entity :as entity]))

(defn- pressed-on-skill-in-menu [{:keys [context/player-entity]
                                  :as context}
                                 skill]
  (when (and (pos? (:entity/free-skill-points @player-entity))
             (not (entity/has-skill? @player-entity skill)))
    (swap! player-entity update :entity/free-skill-points dec)
    (add-skill! context player-entity skill)))

; TODO render text label free-skill-points
; (str "Free points: " (:entity/free-skill-points @player-entity))
(defn create [context]
  (->window context
            {:title "Skills"
             :id :skill-window
             :visible? false
             :cell-defaults {:pad 10}
             :rows [(for [id [:spells/projectile
                              :spells/meditation
                              :spells/spawn]
                          :let [; get-property in callbacks if they get changed, this is part of context permanently
                                button (->image-button context
                                                       (:property/image (get-property context id)) ; TODO here anyway taken
                                                       ; => should probably build this window @ game start
                                                       (fn [{:keys [context/player-entity] :as ctx}]
                                                         (when (= :idle (entity/state @player-entity))
                                                           (pressed-on-skill-in-menu ctx (get-property ctx id)))))]]
                      (do
                       (add-tooltip! button #(tooltip-text % (get-property % id)))
                       button))]
             :pack? true}))
