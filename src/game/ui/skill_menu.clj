(nsx game.ui.skill-menu
  (:require clojure.set
            [game.components.hp-mana :refer (enough-mana?)]
            [game.components.skills :refer (assoc-skill has-skill? choose-skill)]
            [game.skills.core :as skills]
            [game.player.entity :refer (player-entity)]
            game.effects.spawn ; => creatures.core
            game.effects.restore-hp-mana ; -> components.hp-mana
            game.effects.projectile ; -> entity.projectile ns ?
            game.effects.target-entity))  ; ? refer from items.core ?

(def ^:privat skill-icon-size 48)

; TODO this is actually two modifiers ... skill and free-skill-p
; and can do modifier-text @ tooltip ....
; 'skills' menu is a modifier menu ...
; you can have more general modifiers menus toggling on/off
; passives toggle-able ... wtf / stances ...
(defn- pressed-on-skill-in-menu [skill-id]
  (when (and (pos? (:free-skill-points @player-entity))
             (not (has-skill? @player-entity skill-id)))
    (->! player-entity
         (update :free-skill-points dec)
         (update :skills assoc-skill skill-id))))

(app/on-create
 (def window (ui/window "Skills"))

 (doseq [id [:projectile
             :meditation
             :spawn]
         :let [skill (skills/skills id)
               button (ui/image-button (:image skill)
                                            #(pressed-on-skill-in-menu id))]]
   (.addListener button (ui/text-tooltip #(skills/text id player-entity)))
   (.add window button))
 ; TODO render text label free-skill-points
 ; (str "Free points: " (:free-skill-points @player-entity))
 (.pack window))

#_(app/on-create
 ; TODO move to src/game/ingame_gui where all other buttons are
 ; no actually - ingame-gui has only the gui components abstract
 ; and somewhere else is gui-specific implementations
 (def ^:private freeskillpointsbutton
   (gui/make-imgbutton
    :image (image/create "ui/icons/skills_free.png" :scale gui/buttonscale)
    :location [(+ gui/buttonx-start 300)
               gui/button-y]
    :pressed #(gui/switch-visible skillmenu-frame)
    :tooltip "You have free skill points"
    :parent gui/ingamestate-display
    :visible false)))

#_(defn update-free-skillbutton-button-visibility []
  (gui/set-visible freeskillpointsbutton
                   (pos? (:free-skill-points @player-entity))))
