(ns game.ui.inventory-window
  (:require [data.grid2d :as grid]
            [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.lc :as lc]
            [gdl.graphics.color :as color]
            [gdl.draw :as draw]
            [gdl.graphics.image :as image]
            [gdl.scene2d.ui :as ui]
            [game.session :as session]
            [game.utils.msg-to-player :refer (show-msg-to-player)]
            [game.components.inventory :as inventory]
            [game.items.core :as items]
            [game.player.entity :refer (player-entity)]
            [game.entities.item :as item-entity])
  (:import com.badlogic.gdx.audio.Sound
           com.badlogic.gdx.graphics.Color
           (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Widget Image TextTooltip Window Table)
           com.badlogic.gdx.scenes.scene2d.utils.ClickListener))

(declare ^Window window)

; TODO ! important ! animation & dont put exactly hiding under player
(defn put-item-on-ground [{:keys [assets] :as context}]
  {:pre [(:item-on-cursor @player-entity)]}
  (.play ^Sound (get assets "sounds/bfxr_itemputground.wav"))
  (let [{x 0 y 1 :as posi} (:position @player-entity)
       ; [w _] item-body-dimensions
       ; half-size (/ w tile-width 2)
       ; below-posi [x (+ 0.7 y)] ; put here so player sees that item is put on ground (confusing trying to put heal pot on player)
       ; blocked (blocked-location? below-posi half-size half-size :ground)
        ; blocked location checks if other solid bodies ... if put under player would block from player
        ;_ (println "BLOCKED? " (boolean blocked))
        ;position (if-not blocked below-posi posi)
        ]
    (item-entity/create! posi (:item-on-cursor @player-entity) context))
  (swap! player-entity dissoc :item-on-cursor))

(defn- complain-2h-weapon-and-shield! []
  ;(.play ^Sound (get assets "error.wav"))
  (show-msg-to-player "Two-handed weapon and shield is not possible."))

(defn- clicked-cell [{:keys [assets]} cell]
  (let [entity player-entity
        inventory (:inventory @entity)
        item (get-in inventory cell)
        item-on-cursor (:item-on-cursor @entity)]
    (cond
     ; TRY PICK FROM CELL
     (and (not item-on-cursor)
          item)
     (do
      (.play ^Sound (get assets "sounds/bfxr_takeit.wav"))
      (swap! entity assoc :item-on-cursor item)
      (inventory/remove-item! entity cell))

     item-on-cursor
     (cond
      ; PUT ITEM IN
      (and (not item)
           (inventory/valid-slot? cell item-on-cursor))
      (if (inventory/two-handed-weapon-and-shield-together? inventory cell item-on-cursor)
        (complain-2h-weapon-and-shield!)
        (do
         (.play ^Sound (get assets "sounds/bfxr_itemput.wav"))
         (inventory/set-item! entity cell item-on-cursor)
         (swap! entity dissoc :item-on-cursor)))

      ; INCREMENT ITEM
      (and item
           (inventory/stackable? item item-on-cursor))
      (do
       (.play ^Sound (get assets "sounds/bfxr_itemput.wav"))
       (inventory/stack-item! entity cell item-on-cursor)
       (swap! entity dissoc :item-on-cursor))

      ; SWAP ITEMS
      (and item
           (inventory/valid-slot? cell item-on-cursor))
      (if (inventory/two-handed-weapon-and-shield-together? inventory cell item-on-cursor)
        (complain-2h-weapon-and-shield!)
        (do
         (.play ^Sound (get assets "sounds/bfxr_itemput.wav"))
         (inventory/remove-item! entity cell)
         (inventory/set-item! entity cell item-on-cursor)
         (swap! entity assoc :item-on-cursor item)))))))

(declare ^:private slot->background
         ^:private ^Table table)

(defmodule _
  (lc/create [_ context]
    (.bindRoot #'window (ui/window :title "Inventory"
                                   :id :inventory-window))
    (.bindRoot #'table (ui/table))
    (.pad table (float 2))
    (.add window table)
    (.bindRoot #'slot->background
               (let [sheet (image/spritesheet context "items/images.png" 48 48)]
                 (->> {:weapon   0 ; TODO use a vector?
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
                              (-> (image/get-sprite context sheet [21 (+ y 2)])
                                  :texture
                                  ui/texture-region-drawable
                                  (.tint (color/rgb 1 1 1 0.4)))]))
                      (into {}))))))

(def ^:private cell-size 48)

(def ^:private droppable-color    (color/rgb 0   0.6 0 0.8))
(def ^:private two-h-shield-color (color/rgb 0.6 0.6 0 0.8))
(def ^:private not-allowed-color  (color/rgb 0.6 0   0 0.8))

(defn- draw-cell-rect [drawer x y mouseover? cell]
  (draw/rectangle drawer x y cell-size cell-size Color/GRAY)
  (when-let [item (:item-on-cursor @player-entity)]
    (when mouseover?
      (let [color (if (and (inventory/valid-slot? cell item)
                           (not (inventory/cell-in-use? @player-entity cell))) ; TODO not player-entity but entity
                    (if (inventory/two-handed-weapon-and-shield-together? (:inventory @player-entity) cell item)
                      two-h-shield-color
                      droppable-color)
                    not-allowed-color)]
        (draw/filled-rectangle drawer (inc x) (inc y) (- cell-size 2) (- cell-size 2) color)))))

(import 'com.badlogic.gdx.math.Vector2)

(defn- mouseover? [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor ^Widget []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [{:keys [drawer gui-mouse-position]} (app/current-context)
            ^Widget this this]
        (draw-cell-rect drawer
                        (.getX this)
                        (.getY this)
                        (mouseover? this gui-mouse-position)
                        (read-string (.getName (.getParent this))))))))

(defn- cell-widget ^Group [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (ui/stack)
      (.setName (pr-str cell)) ; TODO ! .setUserObject
      (.addListener (proxy [ClickListener] []
                      (clicked [event x y]
                        (clicked-cell (app/current-context) cell))))
      (.add (draw-rect-actor))
      (.add (doto (ui/image (slot->background slot))
              (.setName "image"))))))

(defn- redo-table []
  (.clear table)
  (doto table .add .add
    (.add (cell-widget :helm))
    (.add (cell-widget :necklace)) .row)
  (doto table .add
    (.add (cell-widget :weapon))
    (.add (cell-widget :chest))
    (.add (cell-widget :cloak))
    (.add (cell-widget :shield)) .row)
  (doto table .add .add
    (.add (cell-widget :leg)) .row)
  (doto table .add
    (.add (cell-widget :glove))
    (.add (cell-widget :rings :position [0 0]))
    (.add (cell-widget :rings :position [1 0]))
    (.add (cell-widget :boot)) .row)
  (doseq [y (range (grid/height (:bag inventory/empty-inventory)))]
    (doseq [x (range (grid/width (:bag inventory/empty-inventory)))]
      (.add table (cell-widget :bag :position [x y])))
    (.row table)))

; TODO placed items are not serialized/loaded.
(def state (reify session/State
             (load! [_ _]
               (redo-table)
               (.pack window))
             (serialize [_])
             (initial-data [_])))

(defn- get-cell-widget ^Group [cell]
  (.findActor table (pr-str cell)))

(defn- get-image-widget ^Image [cell-widget]
  (.findActor ^Group cell-widget "image"))

(defn- set-item-image-in-widget! [cell item]
  (let [cell-widget (get-cell-widget cell)
        image-widget (get-image-widget cell-widget)]
    (.setDrawable image-widget (ui/texture-region-drawable (:texture (:image item))))
    (.addListener cell-widget (ui/text-tooltip #(items/text item)))))

(defn- remove-item-from-widget! [cell]
  (let [cell-widget (get-cell-widget cell)
        image-widget (get-image-widget cell-widget)
        ^TextTooltip tooltip (first (filter #(instance? TextTooltip %)
                                            (.getListeners cell-widget)))]
    (.setDrawable image-widget (slot->background (cell 0)))
    (.hide tooltip)
    (.removeListener cell-widget tooltip)))

(intern 'game.components.inventory 'set-item-image-in-widget! set-item-image-in-widget!)
(intern 'game.components.inventory 'remove-item-from-widget! remove-item-from-widget!)
