(ns game.ui.inventory-window
  (:require [data.grid2d :as grid]
            [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.graphics.color :as color]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.image :as image]
            [gdl.graphics.gui :as gui]
            [gdl.audio :as audio]
            [gdl.scene2d.ui :as ui]
            [game.utils.random :refer :all]
            [game.session :as session]
            [game.properties :as properties]
            [game.utils.random :refer (get-rand-weighted-item)]
            [game.utils.msg-to-player :refer (show-msg-to-player)]
            [game.modifier :as modifier]
            [game.components.clickable :as clickable]
            [game.components.inventory :as inventory :refer [item-in-hand]]
            [game.items.core :as items]
            [game.player.entity :refer (player-entity)]
            [game.entities.item :as item-entity]
            ;game.components.glittering
            game.components.image
            [game.ui.config :as ui-config])
  (:import com.badlogic.gdx.graphics.g2d.TextureRegion
           [com.badlogic.gdx.scenes.scene2d Actor Group]
           [com.badlogic.gdx.scenes.scene2d.ui Widget Image TextTooltip Window Table]
           [com.badlogic.gdx.scenes.scene2d.utils ClickListener]))

; TODO it is just a grid ... if you look at it .. simplify? just hashmap ?
; also empty inventory can  just be an empty hashmap??
; think of serialization also save/load game...
; also editor -> entities have in different slots items, not just :items
; actually the data structure is ok because :bag we treat the others the same..

; TODO define inventory in 1 place -> slots, render-posi, background-image then take at diff. places !

(declare ^Window window)
; TODO just function and call @ screen (but used @ session !)
; => session part of module ? internal state ?

; TODO ! important ! animation & dont put exactly hiding under player
(defn put-item-on-ground []
  {:pre [(inventory/is-item-in-hand?)]}
  (audio/play "bfxr_itemputground.wav")
  (let [{x 0 y 1 :as posi} (:position @player-entity)
       ; [w _] item-body-dimensions
       ; half-size (/ w tile-width 2)
       ; below-posi [x (+ 0.7 y)] ; put here so player sees that item is put on ground (confusing trying to put heal pot on player)
       ; blocked (blocked-location? below-posi half-size half-size :ground)
        ; blocked location checks if other solid bodies ... if put under player would block from player
        ;_ (println "BLOCKED? " (boolean blocked))
        ;position (if-not blocked below-posi posi)

        ]
    (item-entity/create! posi @item-in-hand))
  (inventory/empty-item-in-hand))

(defn- complain-2h-weapon-and-shield! []
  ;(audio/play "error.wav")
  (show-msg-to-player "Two-handed weapon and shield is not possible."))

; TODO usable items -> no 'belt' but add to action-bar with skill/action-modifier
; & remove AFTER used

#_(defmacro def-usable-item [namestr & {:keys [effect info image use-sound]}]
  `(defitem ~namestr [{cnt# :count}]
     {:effect ~effect
      :use-sound ~use-sound
      :info (str "Use: Rightclick or in belt with hotkeys.\n" ~info)
      :type :usable
      :image ~image
      :count (or cnt# 1)}))

; used only @ grenade(deleted) -> skill based items
#_(defn remove-one-item-from-belt [item-name]
    {:pre [(some #{item-name}
                 (map :id
                      (slot->items inventory :belt)))]}
    (remove-one-item
     (find-first
      #(= item-name (:id %))
      (slot->items inventory :belt))))

(defn- try-usable-item-effect [{:keys [effect use-sound] :as item} cell]
  (if (effect)
    (do
      (inventory/remove-one-item cell)
      (audio/play use-sound))
    (audio/play "bfxr_denied.wav")))

#_(defn update-items []
  #_(let [mouseover-cell (get-mouseover-cell)
          mouseover-item mouseover-cell]
      (when
        (and mouseover-cell
             (and (not (is-rightm-consumed?))
                  (try-consume-rightm-pressed))
             (= (:type mouseover-item) :usable)
             (inventory/cell-in-use? entity* mouseover-cell))
        (try-usable-item-effect mouseover-item mouseover-cell))))

;; TODO this out of here, is not inventory but rand-items

; TODO out of date (move-speed )
(def ^:private item-type-boni
  {"Armor" [:mana :hp :move-speed :cast-speed :spell-crit :perc-dmg-spell]
   "Sword" [:perc-dmg :min-dmg :max-dmg :attack-speed :hp-leech :mana-leech :melee-crit :spell-crit
            :melee-chance-reduce-armor :melee-chance-slow :melee-chance-stun :perc-dmg-spell]
   "Ring" [:armor :mana-reg :attack-speed :move-speed :hp-leech :mana-leech :melee-crit :spell-crit]})

(defn- level->modifier-value
  [level modifier-type]
  {:pre [(#{0 1 2} level)
         (contains? modifier/modifier-definitions modifier-type)]}
  (if-let [values (-> modifier/modifier-definitions
                      modifier-type
                      :values)]
    (-> values
        (nth level)
        rand-int-between)))

(defn- random-modifiers
  [itemname cnt & {max-level :max-level :or {max-level 3}}] ; TODO 3 hardcoded
  (map
   #(vector %
            (level->modifier-value (rand-int max-level) %))
   (take cnt
         (shuffle
          (get item-type-boni itemname)))))

(let [item-names {"Ring"  1,
                  ;"Sword" 1,
                  "Armor" 1}
      boni-counts {1 80,
                   2 15,
                   3 5}]

  (defn- gen-rand-item [max-lvl]
    (let [itemname (get-rand-weighted-item item-names)
          #_item #_(create-item-instance
                itemname
                {:lvl (rand-int max-lvl)})
          item {}
          extra-modifiers (random-modifiers
                           itemname
                           (get-rand-weighted-item boni-counts)
                           :max-level max-lvl)]
      (-> item
          (update :modifiers concat extra-modifiers)
          (assoc :color items/modifiers-text-color))))) ; TODO make @ somehere else ? or based on idk

#_(defnks create-rand-item [position :max-lvl]
    (item-entity/create! position
                         (gen-rand-item max-lvl)))

; DEFCOMPONENT INVENTORY ! this all is !!
; remove any GUI stuff => just a watcher or something....
; => add item-on-cursor for player...


; -> make code like your comments
(comment
 (or (check-pick-item)
     (check-put-item)
     (check-increment-item)
     (check-swap-item))
 )
(defn- clicked-cell [cell]
  (let [entity player-entity
        inventory (:inventory @entity)
        item (get-in inventory cell)]
    (cond
     ; TRY PICK FROM CELL
     (and (not (inventory/is-item-in-hand?))
          item)
     (do
      (audio/play "bfxr_takeit.wav")
      (inventory/set-item-in-hand item)
      (inventory/remove-item entity cell))

     (inventory/is-item-in-hand?)
     (cond
      ; PUT ITEM IN
      (and (not item)
           (inventory/valid-slot? cell @item-in-hand))
      (if (inventory/two-handed-weapon-and-shield-together? inventory cell @item-in-hand)
        (complain-2h-weapon-and-shield!)
        (do
         (audio/play "bfxr_itemput.wav")
         (inventory/set-item entity cell @item-in-hand)
         (inventory/empty-item-in-hand)))

      ; INCREMENT ITEM
      (and item
           (inventory/stackable? item @item-in-hand))
      (do
       (audio/play "bfxr_itemput.wav")
       (inventory/stack-item entity cell @item-in-hand)
       (inventory/empty-item-in-hand))

      ; SWAP ITEMS
      (and item
           (inventory/valid-slot? cell @item-in-hand))
      (if (inventory/two-handed-weapon-and-shield-together? inventory cell @item-in-hand)
        (complain-2h-weapon-and-shield!)
        (do
         (audio/play "bfxr_itemput.wav")
         (inventory/remove-item entity cell)
         (inventory/set-item entity cell @item-in-hand)
         (inventory/set-item-in-hand item)))))))

(declare ^:private slot->background
         ^:private ^Table table)

(defmodule _
  (lc/create [_]
    (.bindRoot #'window (ui/window :title "Inventory"
                                   :id :inventory-window))
    (.bindRoot #'table (ui/table))
    (.pad table (float 2))
    (.add window table)
    (.bindRoot #'slot->background
               (let [sheet (image/spritesheet "items/images.png" 48 48)]
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
                              (-> sheet
                                  (image/get-sprite [21 (+ y 2)])
                                  :texture
                                  ui/texture-region-drawable
                                  (.tint (color/rgb 1 1 1 0.4)))]))
                      (into {}))))))

(def ^:private cell-size 48)

(def ^:private droppable-color    (color/rgb 0   0.6 0 0.8))
(def ^:private two-h-shield-color (color/rgb 0.6 0.6 0 0.8))
(def ^:private not-allowed-color  (color/rgb 0.6 0   0 0.8))

(defn- draw-cell-rect [x y mouseover? cell]
  (shape-drawer/rectangle x y cell-size cell-size color/gray)
  (when (and @item-in-hand mouseover?)
    (let [color (if (and (inventory/valid-slot? cell @item-in-hand)
                         (not (inventory/cell-in-use? @player-entity cell))) ; TODO not player-entity but entity
                  (if (inventory/two-handed-weapon-and-shield-together? (:inventory @player-entity) cell @item-in-hand)
                    two-h-shield-color
                    droppable-color)
                  not-allowed-color)]
      (shape-drawer/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

(import 'com.badlogic.gdx.math.Vector2)

(defn- mouseover? [^com.badlogic.gdx.scenes.scene2d.Actor actor]
  (let [[x y] (gui/mouse-position)
        v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor ^Widget []
  (proxy [Widget] []
    (draw [batch parent-alpha]
      (let [^Widget this this]
        (draw-cell-rect (.getX this)
                        (.getY this)
                        (mouseover? this)
                        (read-string (.getName (.getParent this))))))))

(defn- cell-widget ^Group [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (ui/stack)
      (.setName (pr-str cell)) ; TODO ! .setUserObject
      (.addListener (proxy [ClickListener] []
                      (clicked [event x y]
                        (clicked-cell cell))))
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

(defn- set-item-image-in-widget [cell item]
  (let [cell-widget (get-cell-widget cell)
        image-widget (get-image-widget cell-widget)]
    (.setDrawable image-widget (ui/texture-region-drawable (:texture (:image item))))
    (.addListener cell-widget (ui/text-tooltip #(items/text item)))))

(defn- remove-item-from-widget [cell]
  (let [cell-widget (get-cell-widget cell)
        image-widget (get-image-widget cell-widget)
        ^TextTooltip tooltip (first (filter #(instance? TextTooltip %)
                                            (.getListeners cell-widget)))]
    (.setDrawable image-widget (slot->background (cell 0)))
    (.hide tooltip)
    (.removeListener cell-widget tooltip)))

(intern 'game.components.inventory 'set-item-image-in-widget set-item-image-in-widget)
(intern 'game.components.inventory 'remove-item-from-widget remove-item-from-widget)
