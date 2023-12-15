(ns game.ui.action-bar
  (:require [gdl.app :as app]
            [gdl.scene2d.ui :as ui]
            [game.skills.core :as skills])
  (:import (com.badlogic.gdx Gdx Input$Keys)
           com.badlogic.gdx.scenes.scene2d.Actor
           (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup ButtonGroup Button)))

; TODO when no selected-skill & new skill assoce'd (sword at start)
; => set selected (or : on click  and no skill selected -> show error / sound ' no skill selected'
; actualizer doesnt do that ?
; TODO actually only weapon skill can be dissoc'ed
; so no need to redo all and can keep idx. 1.
; all get re-shuffled.
; -> first check any skills in actionbar not has-skill? anymore -> just remove them at that index
; -> then check player-skills not in actionbar -> add at an index.
; keep index 1 for item ?
; TODO what if selected skill gets removed
; -> no more selected skill (no wait gets updated)

; if 1 gets removed, keep 2,3,4
; now everything gets re-shuffled
; only remove the one which is removed
; keep empty slot ?
; TODO
; * add imageChecked to style ( for cooldown / selected ) -> make red cooldown / otherwise diff. color
; * or even sector circling for cooldown like in WoW (clipped !)

; * tooltips ! with hotkey-number !
;  ( (skills/text skill-id player-entity))


; * TODO add hotkey number to tooltips
; * TODO hotkeys => select button

(def ^:private slot-keys [:1 :2 :3 :4 :5 :6 :7 :8 :9])

(defn- empty-slot->skill-id []
  (apply sorted-map
         (interleave slot-keys
                     (repeat nil))))

(def selected-skill-id (atom nil))
(def ^:private slot->skill-id (atom nil))

(defn reset-skills! []
  (reset! selected-skill-id nil)
  (reset! slot->skill-id (empty-slot->skill-id)))

; TODO gui-state is not restored (widgets)
#_(def state (reify session/State
             (load! [_ {:keys [selected-skill
                               actionbar]}]
               (reset! selected-skill-id nil)
               (reset! slot->skill-id (empty-slot->skill-id)))
             (serialize [_]
               {:selected-skill @selected-skill-id
                :actionbar      @slot->skill-id})
             (initial-data [_]
               {:selected-skill nil
                :actionbar (empty-slot->skill-id)})))

(defn- add-skill-to-hotbar [skill-id]
  (let [unused-index (first (filter #(nil? (get @slot->skill-id %))
                                    slot-keys))]
    (assert unused-index)
    (swap! slot->skill-id assoc unused-index skill-id)))

(declare check-hotbar-actualize
         ^HorizontalGroup horizontal-group) ; TODO == action-bar

(defn- ->hotbar-actualize-actor []
  (proxy [Actor] []
    (act [_delta]
      (check-hotbar-actualize @app/state))))

(defn initialize! []
  (.bindRoot #'horizontal-group (HorizontalGroup.))
  (.addActor horizontal-group (->hotbar-actualize-actor)))

(declare ^ButtonGroup button-group)

(defn- reset-buttons! [{:keys [context/player-entity]}]
  (.clearChildren horizontal-group)
  (.addActor horizontal-group (->hotbar-actualize-actor))

  (.bindRoot #'button-group (ButtonGroup.))
  (.setMaxCheckCount button-group 1)
  (.setMinCheckCount button-group 0)
  ;(.setUncheckLast button-group true) ? needed ?

  (doseq [[id {:keys [image]}] (:skills @player-entity)
          :let [button (ui/image-button image #(reset! selected-skill-id id))]]
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

(defn- check-hotbar-actualize [{:keys [context/player-entity] :as context}]
  (let [player-skills (:skills @player-entity)]
    (when-not (= (set (keys player-skills))
                 (set (vals @slot->skill-id)))
      (reset! slot->skill-id nil)
      (when-not (contains? player-skills @selected-skill-id)
        (reset! selected-skill-id nil))
      (doseq [skill-id (keys player-skills)]
        (add-skill-to-hotbar skill-id))
      (reset-buttons! context))))

(defn- number-str->input-key [number-str]
  (eval (symbol (str "com.badlogic.gdx.Input$Keys/NUM_" number-str))))

(defn up-skill-hotkeys []
  (doseq [slot slot-keys
          :let [skill-id (slot @slot->skill-id)]
          :when (and (.isKeyJustPressed Gdx/input (number-str->input-key (name slot)))
                     skill-id)]
    (.setChecked ^Button (.findActor horizontal-group (str skill-id)) true)))

(comment
 (.getChildren horizontal-group)
 ;#object[com.badlogic.gdx.utils.SnapshotArray 0x5d081309 "[Actor$ff19274a, :sword, :projectile, :meditation, :spawn]"]

 )
