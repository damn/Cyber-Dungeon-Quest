(ns context.ui.inventory-window
  (:require [clojure.string :as str]
            [data.grid2d :as grid]
            [gdl.app :refer [current-context]]
            [gdl.graphics.color :as color]
            [gdl.context :refer [draw-rectangle draw-filled-rectangle spritesheet get-sprite
                                 play-sound! gui-mouse-position get-stage ->text-tooltip]]
            [gdl.scene2d.ui :as ui :refer [find-actor-with-id]]
            [game.entity :as entity]
            [game.context :refer [show-msg-to-player! send-event! modifier-text set-item! stack-item! remove-item!]]
            [context.entity.inventory :as inventory])
  (:import com.badlogic.gdx.graphics.Color
           (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Widget Image TextTooltip Window Table)
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

(defn- slot->background [context]
  (let [sheet (spritesheet context "items/images.png" 48 48)]
    (->> {:weapon   0
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
                 (-> (get-sprite context sheet [21 (+ y 2)])
                     :texture
                     ui/texture-region-drawable
                     (.tint (Color. (float 1) (float 1) (float 1) (float 0.4))))]))
         (into {}))))

(defn ->inventory-window [{:keys [context/inventory] :as context}]
  (let [window (ui/window :title "Inventory"
                          :id :inventory-window)
        table (ui/table)]
    (reset! inventory {:window window
                       :slot->background (slot->background context)
                       :table table})
    (.pad table (float 2))
    (.add window table)
    window))

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
                        (read-string (.getName (.getParent this))))))))

(defn- cell-widget ^Group [slot->background slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (ui/stack)
      (.setName (pr-str cell)) ; TODO ! .setUserObject
      (.addListener (proxy [ClickListener] []
                      (clicked [event x y]
                        (clicked-cell @current-context cell))))
      (.add (draw-rect-actor))
      (.add (doto (ui/image (slot->background slot))
              (.setName "image"))))))

(defn- get-cell-widget ^Group [table cell]
  (.findActor table (pr-str cell)))

(defn- get-image-widget ^Image [cell-widget]
  (.findActor ^Group cell-widget "image"))

; TODO write 'two handed' at weapon info -> key-to-pretty-tooltip-text function for keywords (extend-c?)
(defn- item-name [item]
  (str (:pretty-name item)
       (when-let [cnt (:count item)]
         (str " (" cnt ")"))))

(defn- item-text [context item]
  (str (item-name item) "\n"
       (->> item
            :modifier
            (modifier-text context))))

(defn- redo-table [{:keys [table slot->background]}]
  (let [->cell (fn [& args]
                 (apply cell-widget slot->background args))]
    (.clear table)
    (doto table .add .add
      (.add (->cell :helm))
      (.add (->cell :necklace)) .row)
    (doto table .add
      (.add (->cell :weapon))
      (.add (->cell :chest))
      (.add (->cell :cloak))
      (.add (->cell :shield)) .row)
    (doto table .add .add
      (.add (->cell :leg)) .row)
    (doto table .add
      (.add (->cell :glove))
      (.add (->cell :rings :position [0 0]))
      (.add (->cell :rings :position [1 0]))
      (.add (->cell :boot)) .row)
    (doseq [y (range (grid/height (:bag inventory/empty-inventory)))]
      (doseq [x (range (grid/width (:bag inventory/empty-inventory)))]
        (.add table (->cell :bag :position [x y])))
      (.row table))))

(extend-type gdl.context.Context
  game.context/InventoryWindow
  (inventory-window-visible? [{:keys [context/inventory]}]
    (.isVisible ^Actor (:window @inventory)))

  (rebuild-inventory-widgets [{:keys [context/inventory]}]
    (redo-table @inventory)
    (.pack (:window @inventory)))

  (set-item-image-in-widget [{:keys [context/inventory] :as ctx} cell item]
    (let [cell-widget (get-cell-widget (:table @inventory) cell)
          image-widget (get-image-widget cell-widget)]
      (.setDrawable image-widget (ui/texture-region-drawable (:texture (:image item))))
      (.addListener cell-widget (->text-tooltip ctx #(item-text % item)))))

  (remove-item-from-widget [{:keys [context/inventory]} cell]
    (let [cell-widget (get-cell-widget (:table @inventory) cell)
          image-widget (get-image-widget cell-widget)
          ^TextTooltip tooltip (first (filter #(instance? TextTooltip %)
                                              (.getListeners cell-widget)))]
      (.setDrawable image-widget ((:slot->background @inventory) (cell 0)))
      (.hide tooltip)
      (.removeListener cell-widget tooltip))))

(defn ->context []
  {:context/inventory (atom nil)})
