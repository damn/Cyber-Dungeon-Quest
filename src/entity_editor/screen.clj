(ns entity-editor.screen
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.app :as app]
            [gdl.input :as input]
            [gdl.graphics.gui :as gui]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ui :as ui]
            [game.entities.creature :as creatures]
            [game.items.core :as items]
            [game.skills.core :as skills]
            [game.effects.core :as effects]
            [game.components.modifiers :as modifiers])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Stage)
           (com.badlogic.gdx.scenes.scene2d.ui SplitPane Table)
           (com.badlogic.gdx.scenes.scene2d.utils TextureRegionDrawable)))

; TODO search for ui/table & ui/window (add rows,etc to window too)
; => all widgets set-opts based on class & supers?
; => just (-> (Constructor.) (set-opts opts))
; => (ui/show-opts class)
; but we dont want class but 'table' ??
; also search for com.badlogic

; TODO editing the 'loaded' stuff, not from edn

; abstraction -> properties/get :id / properties/save! :id
; and properties/get-all :namespace
; I dont want to know where/what
; also for entity, for each attribute define widgets, etc.

(defn- editor-window [& {:keys [title
                                id
                                image-widget
                                props-widget-pairs]}]
  (let [window (ui/window :title title
                          :modal? true)]
    (ui/add-rows window
                 (concat
                  [[{:actor image-widget         :colspan 2}]
                   [{:actor (ui/label (name id)) :colspan 2}]]
                  (for [[label value-widget] props-widget-pairs]
                    [(ui/label label) value-widget])
                  [[(ui/text-button "Save" (fn [] ))
                    (ui/text-button "Cancel" #(actor/remove window))]]))
    (ui/pack window)
    (actor/set-center window
                      (/ (gui/viewport-width)  2)
                      (/ (gui/viewport-height) 2))
    window))

(defn creature-type-editor [creature-type-id]
  (let [props (get creatures/creature-types creature-type-id)]
    (editor-window
     :title "Creature Type"
     :id (:id props)
     :image-widget
     (ui/table :rows (map
                      #(map (fn [props] (ui/image (TextureRegionDrawable. (:texture (:image props))))) %)
                      (partition-all 5
                                     (filter #(= (:creature-type %) creature-type-id)
                                             (vals creatures/creatures)))))
     :props-widget-pairs
     [["hp"    (ui/text-field (str (:hp props)))]
      ["speed" (ui/text-field (str (:speed props)))]])))

; attributes of an entity
; * text-editor
; * 'link' => button to edit that connected entity (creature type)
; * 'list' => items/skills/modifiers/etc.
;  => define per entity base (but I do anyway through functions?)

; TODO
; * define widgets per attribute (e.g. id - not editable - label)
; * => need to define entity-type-attribute-keysets
; * what about links ? will become buttons ? schema? datomic?
; save/undo/redo => datomic?

(defn- stage []
  (:stage (app/current-screen-value)))

(defn- split-pane ^SplitPane []
  (:split-pane (app/current-screen-value)))

(defn- creature-editor [id]
  (let [props (get creatures/creatures id)]
    (editor-window
     :title "Creature"
     :id (:id props)
     :image-widget (ui/image (TextureRegionDrawable. (:texture (:image props))))
     :props-widget-pairs
     [["type" (ui/text-button (name (:creature-type props)) (fn []
                                                              (.addActor
                                                               (stage)
                                                               (creature-type-editor (:creature-type props)))))]
      ["level"  (ui/text-field (str (:level props)))]
      ["skills" (ui/table :rows (concat
                                 (for [skill (:skills props)]
                                   [(ui/image (TextureRegionDrawable. (:texture (:image (get skills/skills skill)))))
                                    (ui/text-button " - " (fn [] (println "Remove " )))])
                                 [[(ui/text-button " + " (fn [] (println "Add ")))]]))]
      ["items"  (ui/table :rows (concat
                                 (for [item (:items props)]
                                   [(ui/image (TextureRegionDrawable. (:texture (:image (get items/items item)))))
                                    (ui/text-button " - " (fn [] (println "Remove " )))])
                                 [[(ui/text-button " + " (fn [] (println "Add ")))]]))]])))

; entity-type 'item'
; => edit entity with id
; => where to read properties?
; items => items/items
; 'image-widget' => :image property

(defn- item-editor [id]
  (let [props (get items/items id)]
    (editor-window
     :title "Item"
     :id (:id props)
     :image-widget (ui/image (TextureRegionDrawable. (:texture (:image props))))
     :props-widget-pairs
     [["slot" (ui/label (name (:slot props)))]])))

; TODO effect, first choose from list which one
; then edit properties (different for each effect => depends on params !)
; param types ... e.g. damage.

(defn- skill-editor [id]
  (let [props (get skills/skills id)]
    (editor-window
     :title "Skill"
     :id (:id props)
     :image-widget (ui/image (TextureRegionDrawable. (:texture (:image props))))
     :props-widget-pairs
     [["action-time" (ui/text-field (str (:action-time props)))]
      ["cooldown"    (ui/text-field (str (:cooldown props)))]
      ["cost"        (ui/text-field (str (:cost props)))]
      ["effect"      (ui/text-field (str (:effect props)))]])))

; TODO show hp/speed & with multipler for creatures too ?


; creature-type -> button to the creature type editor itself
; creature-types => also show?
; level
; skills
; items

(defn- entities-table [& {:keys [title
                                 entities
                                 entity-editor
                                 extra-infos-widget]}]
  (let [number-columns 20]
    (ui/table :rows (concat [[{:actor (ui/label title) :colspan number-columns}]]
                            (for [entities (partition-all number-columns entities)]
                              (for [props entities
                                    :let [button (ui/image-button (:image props)
                                                                  #(.addActor (stage) (entity-editor (:id props))))
                                          ^Actor top-widget (extra-infos-widget props)
                                          stack (ui/stack)] ]
                                (do (actor/set-touchable top-widget :disabled)
                                    (.add stack button)
                                    (.add stack top-widget)
                                    stack)))))))

(defn- creatures-table []
  (entities-table :title "Creatures"
                  :entities (sort-by #(vector (or (:level %) 9)
                                              (name (:creature-type %)))
                                     (vals creatures/creatures))
                  :entity-editor creature-editor
                  :extra-infos-widget #(ui/label (or (str (:level %) "-")))))


(defn- items-table []
  (entities-table :title "Items"
                  :entities (sort-by (fn [props]
                                       ;(str/join "-" (reverse (str/split (name (:id props)) #"-")))
                                       (if-let [slot (:slot props)]
                                              (name slot)
                                              ""
                                              )

                                       )
                                     (vals items/items))
                  :entity-editor item-editor
                  :extra-infos-widget (fn [_] (ui/label ""))))

(defn- skills-table []
  (entities-table :title "Skills"
                  :entities (vals skills/skills)
                  :entity-editor skill-editor
                  :extra-infos-widget (fn [_] (ui/label ""))))

 ; TODO
 ; => non-toggle image-button

(defn- left-widget []
  (ui/table :rows [[(ui/text-button "Creatures" #(.setSecondWidget (split-pane) (creatures-table)))]
                   [(ui/text-button "Items"     #(.setSecondWidget (split-pane) (items-table)))]
                   [(ui/text-button "Skills"    #(.setSecondWidget (split-pane) (skills-table)))]
                   [(ui/text-button "Back to Main Menu" #(app/set-screen :game.screens.main))]]))

(defmodule {:keys [stage]}
  (lc/create [_]
    (let [stage (ui/stage)
          split-pane (ui/split-pane :first-widget (left-widget)
                                    :second-widget (creatures-table)
                                    :vertical? false
                                    :id :split-pane)
          table (ui/table :rows [[split-pane]]
                          :fill-parent? true)]
      (.addActor stage table)
      {:stage stage
       :split-pane split-pane})) ; TODO only stage needed, can get split-pane through table
  (lc/dispose [_] (.dispose stage))
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_] (gui/render #(ui/draw-stage stage)))
  (lc/tick [_ delta] (ui/update-stage stage delta)))
