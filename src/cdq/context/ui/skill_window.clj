(ns cdq.context.ui.skill-window
  (:require [gdl.context :refer [->window ->image-button]]
            [gdl.scene2d.actor :refer [add-tooltip!]]
            [cdq.context :refer [get-property tooltip-text transact-all!]]
            [cdq.entity :as entity]
            [cdq.state :as state]))

(defn- clicked-skill [{:keys [context/player-entity] :as ctx} id]
  (let [entity* @player-entity]
    (state/clicked-skillmenu-skill (entity/state-obj entity*)
                                   (get-property ctx id)
                                   entity*
                                   ctx)))

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
                                                       (fn [ctx]
                                                         (clicked-skill ctx id)
                                                         ; TODO
                                                         #_(transact-all! (clicked-skill ctx id) ctx)))]]
                      (do
                       (add-tooltip! button #(tooltip-text % (get-property % id)))
                       button))]
             :pack? true}))
