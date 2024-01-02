(ns context.ui.inventory-window
  (:require [clojure.string :as str]
            [data.grid2d :as grid]
            [gdl.app :refer [current-context]]
            [gdl.context :refer [draw-rectangle draw-filled-rectangle spritesheet get-sprite
                                 play-sound! gui-mouse-position get-stage ->table ->window
                                 ->texture-region-drawable ->color ->stack ->image-widget ->image-button]]
            [gdl.graphics.color :as color]
            [gdl.scene2d.actor :as actor :refer [set-id! add-listener! set-name! add-tooltip! remove-tooltip!]]
            [context.entity.inventory :as inventory]
            [cdq.context :refer [show-msg-to-player! send-event! set-item! stack-item! remove-item! get-property tooltip-text]]
            [cdq.entity :as entity])
  (:import com.badlogic.gdx.scenes.scene2d.Actor
           (com.badlogic.gdx.scenes.scene2d.ui Widget Image Window Table)
           com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
           com.badlogic.gdx.scenes.scene2d.utils.ClickListener
           com.badlogic.gdx.math.Vector2))

; diablo2 unique gold rgb 144 136 88
#_(def ^:private gold-item-color [0.84 0.8 0.52])
; diablo2 blue magic rgb 72 80 184
#_(def modifiers-text-color [0.38 0.47 1])

(defn- complain-2h-weapon-and-shield! [context]
  ;(.play ^Sound (get assets "error.wav"))
  (show-msg-to-player! context "Two-handed weapon and shield is not possible."))

(defn- clicked-cell [{:keys [context/player-entity] :as context} cell]
  (let [entity player-entity
        inventory (:inventory @entity)
        item (get-in inventory cell)
        item-on-cursor (:item-on-cursor @entity)]
    (cond
     ; PICKUP FROM CELL
     (and (not item-on-cursor)
          item)
     (do
      (play-sound! context "sounds/bfxr_takeit.wav")
      (send-event! context entity :pickup-item item)
      (remove-item! context entity cell))

     item-on-cursor
     (cond
      ; PUT ITEM IN EMPTY CELL
      (and (not item)
           (inventory/valid-slot? cell item-on-cursor))
      (if (inventory/two-handed-weapon-and-shield-together? inventory cell item-on-cursor)
        (complain-2h-weapon-and-shield! context)
        (do
         (play-sound! context "sounds/bfxr_itemput.wav")
         (set-item! context entity cell item-on-cursor)
         (swap! entity dissoc :item-on-cursor)
         (send-event! context entity :dropped-item)))

      ; STACK ITEMS
      (and item
           (inventory/stackable? item item-on-cursor))
      (do
       (play-sound! context "sounds/bfxr_itemput.wav")
       (stack-item! context entity cell item-on-cursor)
       (swap! entity dissoc :item-on-cursor)
       (send-event! context entity :dropped-item))

      ; SWAP ITEMS
      (and item
           (inventory/valid-slot? cell item-on-cursor))
      (if (inventory/two-handed-weapon-and-shield-together? inventory cell item-on-cursor)
        (complain-2h-weapon-and-shield! context)
        (do
         (play-sound! context "sounds/bfxr_itemput.wav")
         (remove-item! context entity cell)
         (set-item! context entity cell item-on-cursor)
         ; need to dissoc and drop otherwise state enter does not trigger picking it up again
         (swap! entity dissoc :item-on-cursor)
         (send-event! context entity :dropped-item)
         (send-event! context entity :pickup-item item)))))))

; TODO swap item doesnt work => because already item in hand ... so nothing happens not entering the state
; => move item-on-cursor code all together in the state component ns

(def ^:private cell-size 48)

(def ^:private droppable-color    [0   0.6 0 0.8])
(def ^:private two-h-shield-color [0.6 0.6 0 0.8])
(def ^:private not-allowed-color  [0.6 0   0 0.8])

(defn- draw-cell-rect [c player-entity x y mouseover? cell]
  (draw-rectangle c x y cell-size cell-size color/gray)
  (when (and mouseover?
             (= :item-on-cursor (entity/state @player-entity)))
    (let [item (:item-on-cursor @player-entity)
          color (cond
                 (not (inventory/valid-slot? cell item))
                 not-allowed-color

                 (inventory/two-handed-weapon-and-shield-together? (:inventory @player-entity)
                                                                   cell
                                                                   item)
                 two-h-shield-color

                 :else
                 droppable-color)]
      (draw-filled-rectangle c (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

(defn- mouseover? [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor ^Widget []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [{:keys [context/player-entity] :as c} @current-context
            ^Widget this this]
        (draw-cell-rect c
                        player-entity
                        (.getX this)
                        (.getY this)
                        (mouseover? this (gui-mouse-position c))
                        (actor/id (actor/parent this)))))))

(defn- ->cell [ctx slot->background slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (->image-widget ctx
                                     (slot->background slot)
                                     {:id :image})
        stack (->stack ctx [(draw-rect-actor)
                            image-widget])]
    (set-name! stack "inventory-cell")
    (set-id! stack cell)
    (add-listener! stack (proxy [ClickListener] []
                           (clicked [event x y]
                             (let [{:keys [context/player-entity] :as ctx} @current-context]
                               (when (#{:item-on-cursor :idle} (entity/state @player-entity))
                                 (clicked-cell ctx cell))))))
    stack))

(defn- redo-table [ctx {:keys [^Table table slot->background]}]
  ; cannot do add-rows, need bag :position idx
  (let [cell (fn [& args] (apply ->cell ctx slot->background args))]
    (.clear table)
    (doto table .add .add
      (.add ^Actor (cell :inventory.slot/helm))
      (.add ^Actor (cell :inventory.slot/necklace)) .row)
    (doto table .add
      (.add ^Actor (cell :inventory.slot/weapon))
      (.add ^Actor (cell :inventory.slot/chest))
      (.add ^Actor (cell :inventory.slot/cloak))
      (.add ^Actor (cell :inventory.slot/shield)) .row)
    (doto table .add .add
      (.add ^Actor (cell :inventory.slot/leg)) .row)
    (doto table .add
      (.add ^Actor (cell :inventory.slot/glove))
      (.add ^Actor (cell :inventory.slot/rings :position [0 0]))
      (.add ^Actor (cell :inventory.slot/rings :position [1 0]))
      (.add ^Actor (cell :inventory.slot/boot)) .row)
    (doseq [y (range (grid/height (:inventory.slot/bag inventory/empty-inventory)))]
      (doseq [x (range (grid/width (:inventory.slot/bag inventory/empty-inventory)))]
        (.add table ^Actor (cell :inventory.slot/bag :position [x y])))
      (.row table))))

(extend-type gdl.context.Context
  cdq.context/InventoryWindow
  (inventory-window [{{:keys [window]} :context/inventory}]
    window)

  (rebuild-inventory-widgets [{{:keys [^Window window] :as inventory} :context/inventory :as ctx}]
    (redo-table ctx inventory)
    (.pack window))

  (set-item-image-in-widget [{{:keys [table]} :context/inventory :as ctx}
                             cell
                             item]
    (let [^Actor cell-widget (get table cell)
          ^Image image-widget (get cell-widget :image)]
      (.setDrawable image-widget (->texture-region-drawable ctx (:texture (:property/image item))))
      (add-tooltip! cell-widget #(tooltip-text % item))))

  (remove-item-from-widget [{{:keys [table slot->background]} :context/inventory :as ctx} cell]
    (let [^Actor cell-widget (get table cell)
          ^Image image-widget (get cell-widget :image)]
      (.setDrawable image-widget (slot->background (cell 0)))
      (remove-tooltip! cell-widget))))

(defn- slot->background [ctx]
  (let [sheet (spritesheet ctx "items/images.png" 48 48)]
    (->> #:inventory.slot {:weapon   0
                           :shield   1
                           :rings    2
                           :necklace 3
                           :helm     4
                           :cloak    5
                           :chest    6
                           :leg      7
                           :glove    8
                           :boot     9
                           :bag      10} ; transparent
         (map (fn [[slot y]]
                [slot
                 (.tint ^TextureRegionDrawable
                        (->texture-region-drawable ctx
                                                   (:texture (get-sprite ctx sheet [21 (+ y 2)])))
                        (->color ctx 1 1 1 0.4))]))
         (into {}))))

(defn ->inventory-window [{{:keys [window] :as inventory} :context/inventory}]
  window)

(defn ->context [{:keys [gui-viewport-width
                         gui-viewport-height] :as context}]
  ; TODO use ::data ?
  {:context/inventory (let [table (->table context {})]
                        {:window (->window context {:title "Inventory"
                                                    :id :inventory-window
                                                    :visible? false
                                                    :position [gui-viewport-width
                                                               gui-viewport-height]
                                                    :rows [[{:actor table :pad 2 :colspan 2}]
                                                           [(->image-button context (:property/image (get-property context :book-latch))
                                                                            (fn [_]
                                                                              (gdl.app/change-screen! :screens/minimap)))
                                                            (->image-button context  (:property/image (get-property context :key-bone))
                                                                            (fn [_]
                                                                              (gdl.app/change-screen! :screens/options-menu)))]]})
                         :slot->background (slot->background context)
                         :table table})})
