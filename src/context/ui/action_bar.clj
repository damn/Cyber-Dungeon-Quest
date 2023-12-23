(ns context.ui.action-bar
  (:require [gdl.context :refer [->image-button key-just-pressed?]]
            ;[gdl.input :as input] ; TODO
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ui :as ui]
            [app.state :refer [current-context]]
            game.context
            [game.skill :as skill])
  (:import com.badlogic.gdx.scenes.scene2d.Actor
           (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup ButtonGroup Button)))

; TODO
; * cooldown / not usable -> diff. colors ? disable on not able to use skills (stunned?)
; * or even sector circling for cooldown like in WoW (clipped !)
; * tooltips ! with hotkey-number !
;  ( (skills/text skill-id player-entity))
; * add hotkey number to tooltips
; * hotkeys => select button
  ; when no selected-skill & new skill assoce'd (sword at start)
  ; => set selected
  ; keep weapon at position 1 always ?

#_(def ^:private slot-keys {:1 input.keys.num-1
                          :2 input.keys.num-2
                          :3 input.keys.num-3
                          :4 input.keys.num-4
                          :5 input.keys.num-5
                          :6 input.keys.num-6
                          :7 input.keys.num-7
                          :8 input.keys.num-8
                          :9 input.keys.num-9})

#_(defn- empty-slot->skill-id []
  (apply sorted-map
         (interleave slot-keys
                     (repeat nil))))

#_(def selected-skill-id (atom nil))
#_(def ^:private slot->skill-id (atom nil))

#_(defn reset-skills! []
  (reset! selected-skill-id nil)
  (reset! slot->skill-id (empty-slot->skill-id)))


(defn- skill-tooltip-text [{:keys [context/player-entity] :as context} skill]
  (skill/text skill player-entity context))

(defn- ->button-group []
  (let [button-group (ButtonGroup.)]
    (.setMaxCheckCount button-group 1)
    (.setMinCheckCount button-group 0)
    ;(.setUncheckLast button-group true) ? needed ?
    button-group))

(defn ->context []
  {:context.ui/action-bar (atom nil)})

(extend-type gdl.context.Context
  game.context/Actionbar
  (->action-bar [{:keys [context.ui/action-bar]}]
    (let [horizontal-group (HorizontalGroup.)
          button-group (->button-group)]
      (reset! action-bar {:horizontal-group horizontal-group
                          :button-group button-group})
      horizontal-group))

  (reset-actionbar [{:keys [context.ui/action-bar]}]
    (.clearChildren (:horizontal-group @action-bar))
    (.clear         (:button-group     @action-bar)))

  (selected-skill [{:keys [context.ui/action-bar]}]
    (when-let [skill-button (.getChecked (:button-group @action-bar))]
      (actor/id skill-button)))

  (actionbar-add-skill [{:keys [context.ui/action-bar]
                         :as ctx}
                        {:keys [id image] :as skill}]
    (let [button (->image-button ctx image (fn [_context] ))]
      (actor/set-id button id)
      (.addListener button (ui/text-tooltip (fn []
                                              (skill-tooltip-text @current-context
                                                                  skill))))
      (.addActor (:horizontal-group @action-bar) button)
      (.add      (:button-group     @action-bar) button)))

  (actionbar-remove-skill [{:keys [context.ui/action-bar]
                            :as ctx}
                           {:keys [id]}]
    (let [button (ui/find-actor-with-id (:horizontal-group @action-bar) id)]
      (.remove button))))


(comment
 (def sword-button (.getChecked button-group))
 (.setChecked sword-button false)
 )

#_(defn- number-str->input-key [number-str]
  (eval (symbol (str "com.badlogic.gdx.Input$Keys/NUM_" number-str))))

; TODO do with an actor
; .getChildren horizontal-group => in order
(defn up-skill-hotkeys []
  #_(doseq [slot slot-keys
          :let [skill-id (slot @slot->skill-id)]
          :when (and (key-just-pressed? context (number-str->input-key (name slot)))
                     skill-id)]
    (.setChecked ^Button (.findActor horizontal-group (str skill-id)) true)))

(comment


 ; https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/scenes/scene2d/ui/Button.html
 (.setProgrammaticChangeEvents ^Button (.findActor horizontal-group ":spells/spawn") true)
 ; but doesn't toggle:
 (.toggle ^Button (.findActor horizontal-group ":spells/spawn"))
 (.setChecked ^Button (.findActor horizontal-group ":spells/spawn") true)
 ; Toggles the checked state. This method changes the checked state, which fires a ChangeListener.ChangeEvent (if programmatic change events are enabled), so can be used to simulate a button click.

 ; => it _worked_ => active skill changed
 ; only button is not highlighted idk why

 (.getChildren horizontal-group)

 )
