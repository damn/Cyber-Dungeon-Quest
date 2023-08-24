(nsx game.ui.action-bar
  (:require [game.skills.core :as skills]
            [game.components.skills :refer (choose-skill)]
            [game.player.entity :refer (player-entity)])
  (:import (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup ButtonGroup Button)))

; skill-bar?? -> maybe also potions or whatever actions could be included in the future ?!

; use text color codes and newlines working?
; -> no need for custom render-readable-text (mostly @ gui ?)
; anyway dont use textseq -> only newlines !
; because of colors ...
; add them in the text ...
; -> use libgdx font thing

; => game.ui.actionbar
; -=> not skills but actions? actiontime?
; => look properly!
; -> icon for action 'attack with weapon' -> like in D2
; or greyscale the weapon

; TODO when no selected-skill & new skill assoce'd (sword at start)
; => set selected
; actualizer doesnt do that ?

; TODO I could move all this state into player entity
; even the widgets themself and/or add-watch to player-entity
; look at other state which you could move into player-entity
; model/view together is fine,

(def ^:private selected-skill-id (atom nil))

(def ^:private slot->skill-id (atom nil))

(def ^:private slot-keys [:1 :2 :3 :4 :5 :6 :7 :8 :9])

(defn- empty-slot->skill-id []
  (apply sorted-map
         (interleave slot-keys
                     (repeat nil))))

; TODO gui-state is not restored (widgets)
(def state (reify session/State
             (load! [_ {:keys [selected-skill
                               actionbar]}]
               (reset! selected-skill-id selected-skill)
               (reset! slot->skill-id actionbar))
             (serialize [_]
               {:selected-skill @selected-skill-id
                :actionbar      @slot->skill-id})
             (initial-data [_]
               {:selected-skill nil
                :actionbar (empty-slot->skill-id)})))


(defn- player-skills []
  (:skills @player-entity))

(defn- add-skill-to-hotbar [skill-id]
  (let [unused-index (first (filter #(nil? (get @slot->skill-id %))
                                    slot-keys))]
    (assert unused-index)
    (swap! slot->skill-id assoc unused-index skill-id)))

; TODO actually only weapon skill can be dissoc'ed
; so no need to redo all and can keep idx. 1.
; all get re-shuffled.


; -> first check any skills in actionbar not has-skill? anymore -> just remove them at that index
; -> then check player-skills not in actionbar -> add at an index.
; keep index 1 for item ?

(declare check-hotbar-actualize
         ^HorizontalGroup horizontal-group)

(defmodule _
  (lc/create [_]
    (.bindRoot #'horizontal-group (HorizontalGroup.))
    (.addActor horizontal-group (ui/actor check-hotbar-actualize))))

(declare ^ButtonGroup button-group)

(defn- reset-buttons! []
  (.clearChildren horizontal-group)
  (.addActor horizontal-group (ui/actor check-hotbar-actualize))

  (.bindRoot #'button-group (ButtonGroup.))
  (.setMaxCheckCount button-group 1)
  (.setMinCheckCount button-group 0)
  ;(.setUncheckLast button-group true) ? needed ?

  (doseq [[id {:keys [image]}] (player-skills)
          :let [button (ui/image-button image
                                             #(reset! selected-skill-id id))]]
    (.setName button (pr-str id))
    (.addListener button (ui/text-tooltip
                          #(skills/text id player-entity)))
    ; TODO HOTKEY
    (.addActor horizontal-group button)
    (.add button-group button)))

(comment
 (def sword-button (.getChecked button-group))
 (.setChecked sword-button false)
 )

(defn- check-hotbar-actualize []
  (when-not (= (set (keys (player-skills)))
               (set (vals @slot->skill-id)))
    (reset! slot->skill-id nil)
    (when-not (contains? (player-skills) @selected-skill-id)
      (reset! selected-skill-id nil))
    (doseq [skill-id (keys (player-skills))]
      (add-skill-to-hotbar skill-id))
    (reset-buttons!)))

(defn up-skill-hotkeys []
  (doseq [slot slot-keys
          :let [skill-id (slot @slot->skill-id)]
          :when (and (input/is-key-pressed? slot)
                     skill-id)]
    (.setChecked ^Button (.findActor horizontal-group (str skill-id)) true)))

; TODO what if selected skill gets removed
; -> no more selected skill (no wait gets updated)

; if 1 gets removed, keep 2,3,4
; now everything gets re-shuffled
; only remove the one which is removed
; keep empty slot ?

(comment
 (.getChildren horizontal-group)
 ;#object[com.badlogic.gdx.utils.SnapshotArray 0x5d081309 "[Actor$ff19274a, :sword, :projectile, :meditation, :spawn]"]


 )


; TODO
; * add imageChecked to style ( for cooldown / selected ) -> make red cooldown / otherwise diff. color
; * or even sector circling for cooldown like in WoW (clipped !)

; * tooltips ! with hotkey-number !
;  ( (skills/text skill-id player-entity))


; * TODO add hotkey number to tooltips
; * TODO hotkeys => select button
