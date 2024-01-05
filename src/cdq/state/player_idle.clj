(ns cdq.state.player-idle
  (:require [gdl.context :refer [world-mouse-position mouse-on-stage-actor? button-just-pressed? button-pressed?]]
            [gdl.input.buttons :as buttons]
            [gdl.scene2d.actor :refer [visible? toggle-visible! parent] :as actor]
            [gdl.scene2d.ui.button :refer [button?]]
            [gdl.scene2d.ui.window :refer [window-title-bar?]]
            [gdl.math.vector :as v]
            [cdq.context :refer [get-property inventory-window try-pickup-item! skill-usable-state selected-skill]]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.state.wasd-movement :refer [WASD-movement-vector]]))

(defn- denied [text]
  [[:tx/sound "sounds/bfxr_denied.wav"]
   [:tx/msg-to-player text]])

(defmulti ^:private on-clicked
  (fn [_context entity*]
    (:type (:entity/clickable entity*))))

(defmethod on-clicked :clickable/item
  [{:keys [context/player-entity] :as context} clicked-entity*]
  (let [item (:entity/item clicked-entity*)]
    (cond
     (visible? (inventory-window context))
     [(:tx/sound "sounds/bfxr_takeit.wav")
      (assoc clicked-entity* :entity/destroyed? true)
      [:tx/event @player-entity :pickup-item item]]

     (try-pickup-item! context player-entity item)
     [[:tx/sound "sounds/bfxr_pickup.wav"]
      (assoc clicked-entity* :entity/destroyed? true)]

     :else
     [[:tx/sound "sounds/bfxr_denied.wav"]
      [:tx/msg-to-player "Your Inventory is full"]])))

(defmethod on-clicked :clickable/player
  [ctx _clicked-entity*]
  (toggle-visible! (inventory-window ctx)))

(defmethod on-clicked :clickable/princess
  [ctx _clicked-entity*]
  [[:tx/event @(:context/player-entity ctx) :found-princess]])

(defn- clickable->cursor [mouseover-entity* too-far-away?]
  (case (:type (:entity/clickable mouseover-entity*))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag
    :clickable/princess (if too-far-away?
                          :cursors/princess-gray
                          :cursors/princess)))

(def click-distance-tiles 1.5)

(defn- ->clickable-mouseover-entity-interaction [ctx player-entity* mouseover-entity*]
  (if (and (< (v/distance (:entity/position player-entity*)
                          (:entity/position mouseover-entity*))
              click-distance-tiles))
    [(clickable->cursor mouseover-entity* false) (fn [] (on-clicked ctx mouseover-entity*))]
    [(clickable->cursor mouseover-entity* true)  (fn [] (denied "Too far away"))]))

(defn- effect-context [{:keys [context/mouseover-entity] :as context} entity*]
  (let [target @mouseover-entity
        target-position (or (and target (:entity/position @target))
                            (world-mouse-position context))]
    {:effect/source-entity (entity/reference entity*)
     :effect/target-entity target
     :effect/target-position target-position
     :effect/direction (v/direction (:entity/position entity*) target-position)}))

; TODO move to inventory-window extend Context
(defn- inventory-cell-with-item? [{:keys [context/player-entity]} actor]
  (and (parent actor)
       (= "inventory-cell" (actor/name (parent actor)))
       (get-in (:entity/inventory @player-entity)
               (actor/id (parent actor)))))

(defn- mouseover-actor->cursor [ctx]
  (let [actor (mouse-on-stage-actor? ctx)]
    (cond
     (inventory-cell-with-item? ctx actor) :cursors/hand-before-grab
     (window-title-bar? actor) :cursors/move-window
     (button? actor) :cursors/over-button
     :else :cursors/default)))

(defn- ->interaction-state [{:keys [context/mouseover-entity] :as context} entity*]
  (cond
   (mouse-on-stage-actor? context)
   [(mouseover-actor->cursor context)
    (fn []
      nil)] ; handled by actors themself, they check player state

   (and @mouseover-entity
        (:entity/clickable @@mouseover-entity))
   (->clickable-mouseover-entity-interaction context entity* @@mouseover-entity)

   :else
   (if-let [skill-id (selected-skill context)]
     (let [effect-context (effect-context context entity*)
           skill (skill-id (:entity/skills entity*))
           state (skill-usable-state (merge context effect-context) entity* skill)]
       (if (= state :usable)
         (do
          ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
          ; different color ?
          ; => e.g. meditation no TARGET .. etc.
          [:cursors/use-skill
           (fn []
             [[:tx/event entity* :start-action [skill effect-context]]])])
         (do
          ; TODO cursor as of usable state
          ; cooldown -> sanduhr kleine
          ; not-enough-mana x mit kreis?
          ; invalid-params -> depends on params ...
          [:cursors/skill-not-usable
           (fn []
             (denied (case state
                       :cooldown "Skill is still on cooldown"
                       :not-enough-mana "Not enough mana"
                       :invalid-params "Cannot use this here")))])))
     [:cursors/no-skill-selected
      (fn [] (denied "No selected skill"))])))

(defrecord PlayerIdle []
  state/PlayerState
  (player-enter [_])
  (pause-game? [_] true)

  (manual-tick [_ entity* context]
    (if-let [movement-vector (WASD-movement-vector context)]
      [[:tx/event entity* :movement-input movement-vector]]
      (let [[cursor on-click] (->interaction-state context entity*)]
        (cons [:tx/cursor cursor]
              (when (button-just-pressed? context buttons/left)
                (on-click))))))

  state/State
  (enter [_ entity* _ctx])
  (exit  [_ entity* context])
  (tick [_ entity* _context])
  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))
