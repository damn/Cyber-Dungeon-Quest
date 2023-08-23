(nsx game.items.inventory
  (:require [data.grid2d :as grid]
            [game.utils.random :refer :all]
            [x.session :as state]
            [game.properties :as properties]
            [game.utils.random :refer (get-rand-weighted-item)]
            [game.utils.msg-to-player :refer (show-msg-to-player)]
            [game.components.clickable :as clickable]
            [game.items.core :as items]
            [game.player.entity :refer (player-entity)]
            ;game.components.glittering
            game.components.image
            )
  (:import [com.badlogic.gdx.scenes.scene2d.ui Widget Image TextTooltip]
           [com.badlogic.gdx.scenes.scene2d.utils TextureRegionDrawable ClickListener]))

; TODO define inventory in 1 place -> slots, render-posi, background-image then take at diff. places !
(def ^:private empty-inventory
  (->> {:bag      [6 4]
        :weapon   [1 1]
        :shield   [1 1]
        :helm     [1 1]
        :chest    [1 1]
        :leg      [1 1]
        :glove    [1 1]
        :boot     [1 1]
        :cloak    [1 1]
        :necklace [1 1]
        :rings    [2 1]}
       (map (fn [[slot [width height]]]
              [slot
               (grid/create-grid width height (constantly nil))]))
       (into {})))

(defn- cells-and-items [inventory slot]
  (for [[position item] (slot inventory)]
    [[slot position] item]))

(defn- slot->items [inventory slot]
  (-> inventory slot grid/cells))

(defn- cell-exists? [inventory [slot position]]
  (-> inventory slot (contains? position)))

; TODO => valid-cell? allowed-to-put-in-cell?
; => cell = :slot / :position / (later also widget?)
; or widgets check if inventory-changed? then update all buttons ... hmm
; call or observer
; or change listener??
; same for :skills
(defn- valid-slot? [[slot _] item]
  (or (= :bag slot)
      (= (:slot item) slot)))

(defn- two-handed-weapon-and-shield-together? [inventory {slot 0 :as cell} new-item]
  (or (and (:two-handed new-item)
           (= slot :weapon)
           (first (slot->items inventory :shield)))
      (and (= (:slot new-item) :shield)
           (= slot :shield)
           (if-let [weapon (first (slot->items inventory :weapon))]
             (:two-handed weapon)))))

(defn- assoc-inventory-cell! [entity cell item]
  (assoc-in! entity (cons :inventory cell) item))

(defn- applies-modifiers? [[slot _]]
  (not= :bag slot))

(declare set-item-image-in-widget)

(defn- set-item [entity cell item]
  {:pre [(nil? (get-in (:inventory @entity) cell))
         (valid-slot? cell item)
         (not (two-handed-weapon-and-shield-together? (:inventory @entity) cell item))]}
  (when (:is-player @entity)
    (set-item-image-in-widget cell item))
  (assoc-inventory-cell! entity cell item)
  (when (applies-modifiers? cell)
    (modifiers/apply! entity (:modifiers item))))

(declare remove-item-from-widget)

(defn- remove-item [entity cell]
  (let [item (get-in (:inventory @entity) cell)]
    (assert item)
    (when (:is-player @entity)
      (remove-item-from-widget cell))
    (assoc-inventory-cell! entity cell nil)
    (when (applies-modifiers? cell)
      (modifiers/reverse! entity (:modifiers item)))))

(defn- cell-in-use? [entity* cell] ; TODO naming (includes is-active check) ->
  (let [inventory (:inventory entity*)
        item (get-in inventory cell)]
    (and item
         (applies-modifiers? cell)
         (:active-skill? entity*))))

(declare window)

; TODO ui/visible?
(defn- showing-player-inventory? []
   (.isVisible window))

; TODO also move in inventory ? 'item-on-cursor' -> gets saved/loaded with entitydata and reset.
; but keys inventory have to correspond to slots -> at some code.
(def ^:private item-in-hand (atom nil)) ; TODO -on-cursor not in hand
; TODO state for this ??

(defn is-item-in-hand? []
  @item-in-hand)

(defn- set-item-in-hand [item]
  (reset! item-in-hand item))

(defn- empty-item-in-hand []
  (reset! item-in-hand nil))

(defn render-item-in-hand-on-cursor []
  (when @item-in-hand
    (image/draw-centered (:image @item-in-hand) (gui/mouse-position))))

(defn- stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; TODO this is not required but can be asserted, all of one name should have count if others have count
       (= (:name item-a) (:name item-b))))

(defn- stack-item [entity cell item]
  (let [cell-item (get-in (:inventory @entity) cell)]
    (assert (stackable? item cell-item))
    (remove-item entity cell)
    (set-item entity cell (update cell-item :count + (:count item)))))

(defn- remove-one-item [entity cell]
  (let [item (get-in (:inventory @entity) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       (remove-item entity cell)
       (set-item entity cell (update item :count dec)))
      (remove-item entity cell))))

(defn- try-put-item-in
  "returns true when the item was picked up"
  [entity slot item]
  (let [inventory (:inventory @entity)
        cells-items (cells-and-items inventory slot)

        [cell cell-item] (find-first (fn [[cell cell-item]] (stackable? item cell-item))
                                cells-items)
        picked-up (if cell
                    (do (stack-item entity cell item)
                        true)
                    (when-let [[empty-cell] (find-first (fn [[cell item]] (nil? item))
                                                     cells-items)]
                      (when-not (two-handed-weapon-and-shield-together? inventory empty-cell item)
                        (set-item entity empty-cell item)
                        true)))]
    picked-up))

(defn try-pickup-item [entity item]
  (let [slot (find-first #(= % (:slot item))
                         (keys (:inventory @entity)))]
    (or
     (try-put-item-in entity slot item)
     (try-put-item-in entity :bag item))))

(defmethod clickable/on-clicked :item [entity]
  (let [item (:item @entity)]
    (when-not @item-in-hand
      (cond
       (showing-player-inventory?)
       (do
        (audio/play "bfxr_takeit.wav")
        (swap! entity assoc :destroyed? true)
        (set-item-in-hand item))

       (try-pickup-item player-entity item)
       (do
        (audio/play "bfxr_pickup.wav")
        (swap! entity assoc :destroyed? true))

       :else
       (do
        (audio/play "bfxr_denied.wav")
        ; (msg-to-player/show "Your inventory is full")
        (show-msg-to-player "Your Inventory is full"))))))

; -> entities ? w. creatures & projectiles
; TODO use image w. shadows spritesheet
(defn ^:private item-entity [position item]
  (create-entity!
   {:position position
    :body {:width 0.5 ; TODO use item-body-dimensions
           :height 0.5
           :is-solid false ; solid? collides?
           }
    :z-order :on-ground
    :image (:image item)
    ;:glittering true ; no animation (deleted it) TODO ?? (didnt work with pausable)
    :item item
    ; :mouseover-text (:pretty-name item)
    ; :clickable :item
    :clickable {:type :item
                :text (:pretty-name item)}})) ; TODO item-color also from text...? uniques/magic/...

(defn create-item-body [position item]
  (item-entity position
               item
               #_(if (string? item)
                 #_(create-item-instance item) ; TODO item instance
                 item)))

; TODO ! important ! animation & dont put exactly hiding under player
(defn put-item-on-ground []
  {:pre [(is-item-in-hand?)]}
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
    (create-item-body posi @item-in-hand))
  (empty-item-in-hand))

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
                 (map :name
                      (slot->items inventory :belt)))]}
    (remove-one-item
     (find-first
      #(= item-name (:name %))
      (slot->items inventory :belt))))

(defn- try-usable-item-effect [{:keys [effect use-sound] :as item} cell]
  (if (effect)
    (do
      (remove-one-item cell)
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
             (cell-in-use? entity* mouseover-cell))
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
         (contains? modifiers/modifier-definitions modifier-type)]}
  (if-let [values (-> modifiers/modifier-definitions
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
  (create-item-body position
                    (gen-rand-item max-lvl)))

; after-create-entity because try-pickup-item applies modifiers (skillmanager has to be initialised)
(defcomponent :items items
  (after-create! [_ entity]
    (swap! entity assoc :inventory empty-inventory)
    ;(swap! entity dissoc :items)
    (doseq [id items]
      (try-pickup-item entity (id items/items)))))

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
     (and (not (is-item-in-hand?))
          item)
     (do
      (audio/play "bfxr_takeit.wav")
      (set-item-in-hand item)
      (remove-item entity cell))

     (is-item-in-hand?)
     (cond
      ; PUT ITEM IN
      (and (not item)
           (valid-slot? cell @item-in-hand))
      (if (two-handed-weapon-and-shield-together? inventory cell @item-in-hand)
        (complain-2h-weapon-and-shield!)
        (do
         (audio/play "bfxr_itemput.wav")
         (set-item entity cell @item-in-hand)
         (empty-item-in-hand)))

      ; INCREMENT ITEM
      (and item
           (stackable? item @item-in-hand))
      (do
       (audio/play "bfxr_itemput.wav")
       (stack-item entity cell @item-in-hand)
       (empty-item-in-hand))

      ; SWAP ITEMS
      (and item
           (valid-slot? cell @item-in-hand))
      (if (two-handed-weapon-and-shield-together? inventory cell @item-in-hand)
        (complain-2h-weapon-and-shield!)
        (do
         (audio/play "bfxr_itemput.wav")
         (remove-item entity cell)
         (set-item entity cell @item-in-hand)
         (set-item-in-hand item)))))))

(app/defmanaged ^:private slot->background
  (let [sheet (image/spritesheet "items/images.png" 48 48)]
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
                 (-> sheet
                     (image/get-sprite [21 (+ y 2)])
                     :texture
                     TextureRegionDrawable.
                     (.tint (color/rgb 1 1 1 0.4)))]))
         (into {}))))

(def ^:private cell-size 48)

(def- droppable-color    (color/rgb 0   0.6 0 0.8))
(def- two-h-shield-color (color/rgb 0.6 0.6 0 0.8))
(def- not-allowed-color  (color/rgb 0.6 0   0 0.8))

(defn- draw-cell-rect [x y mouseover? cell]
  (shape-drawer/rectangle x y cell-size cell-size color/gray)
  (when (and @item-in-hand mouseover?)
    (let [color (if (and (valid-slot? cell @item-in-hand)
                         (not (cell-in-use? @player-entity cell))) ; TODO not player-entity but entity
                  (if (two-handed-weapon-and-shield-together? (:inventory @player-entity) cell @item-in-hand)
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
(defn- draw-rect-actor []
  (proxy [Widget] []
    (draw [batch parent-alpha]
      (draw-cell-rect (.getX this)
                      (.getY this)
                      (mouseover? this)
                      (read-string (.getName (.getParent this)))))))

(defn- cell-widget [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (ui/stack)
      (.setName (pr-str cell))
      (.addListener (proxy [ClickListener] []
                      (clicked [event x y]
                        (clicked-cell cell))))
      (.add (draw-rect-actor))
      (.add (doto (Image. (slot->background slot))
              (.setName "image"))))))

(app/on-create
 (def window (ui/window :title "Inventory"))
 (def ^:private table (ui/table)) ; TODO top level def, private doesnt work
 (.pad table (float 2))
 (.add window table))

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
  (doseq [y (range (grid/height (:bag empty-inventory)))]
    (doseq [x (range (grid/width (:bag empty-inventory)))]
      (.add table (cell-widget :bag :position [x y])))
    (.row table)))

; TODO placed items are not serialized/loaded.

#_(defcomponent :inventory-window
  (session/load! [_c initial-data]
    (redo-table)
    (.pack window)))

(def state (reify session/State
             (load! [_ _]
               (redo-table)
               (.pack window))
             (serialize [_])
             (initial-data [_])))

(defn- get-cell-widget [cell]
  (.findActor table (pr-str cell)))

(defn- get-image-widget [cell-widget]
  (.findActor cell-widget "image"))

(defn- set-item-image-in-widget [cell item]
  (let [cell-widget (get-cell-widget cell)
        image-widget (get-image-widget cell-widget)]
    (.setDrawable image-widget (TextureRegionDrawable. (:texture (:image item))))
    (.addListener cell-widget (ui/text-tooltip #(items/text item)))))

(defn- remove-item-from-widget [cell]
  (let [cell-widget (get-cell-widget cell)
        image-widget (get-image-widget cell-widget)
        tooltip (first (filter #(instance? TextTooltip %)
                               (.getListeners cell-widget)))]
    (.setDrawable image-widget (slot->background (cell 0)))
    (.hide tooltip)
    (.removeListener cell-widget tooltip)))
