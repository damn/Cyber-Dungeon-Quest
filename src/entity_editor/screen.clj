(ns entity-editor.screen
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.app :as app]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.graphics.gui :as gui]
            [game.entities.creature :as creatures]
            [game.items.core :as items]
            [game.skills.core :as skills]
            [game.effects.core :as effects]
            [game.components.modifiers :as modifiers])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Stage)
           (com.badlogic.gdx.scenes.scene2d.ui SplitPane Table)
           (com.badlogic.gdx.scenes.scene2d.utils TextureRegionDrawable)))

; TODO editing the 'loaded' stuff, not from edn

(defn add-rows [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row
            :let [^Actor actor (if (vector? props-or-actor)
                                 (first props-or-actor)
                                 props-or-actor)
                  cell (.add table actor)]]
      (if (vector? props-or-actor)
        (let [{:keys [colspan]} (rest props-or-actor)]
          (doto cell
            (.colspan colspan)))))
    (.row table)))

(defn mk-table [rows]
  (let [table (ui/table)]
    (add-rows table rows)
    table))

#_(defn mk-window [& {:keys [rows] :as opts}]
  (let [window (apply ui/window opts)]
    (add-rows window rows)
    window))

(defn- editor-window [& {:keys [title
                                id
                                image-widget
                                props-widget-pairs]}]
  (let [table (ui/window :title title
                         :modal? true)]
    (add-rows table [[[image-widget         :colspan 2]]
                     [[(ui/label (name id)) :colspan 2]]])
    (add-rows table (for [[label value-widget] props-widget-pairs]
                      [(ui/label label) value-widget]))
    (add-rows table [[nil (ui/text-button "Cancel" #(.remove table))]])
    (.pack table)
    (ui/set-center-screen table)
    table))

(defn creature-type-editor [creature-type-id]
  (let [props (get creatures/creature-types creature-type-id)]
    (editor-window
     :title "Creature Type"
     :id (:id props)
     :image-widget
     (mk-table
      (map
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
      ["skills" (let [table (ui/table)]
                  (doseq [skill (:skills props)]
                    (.add table (ui/image (TextureRegionDrawable. (:texture (:image (get skills/skills skill))))))
                    (.add table (ui/text-button " - " (fn [] (println "Remove " ))))
                    (.row table))
                  (.add table (ui/text-button " + " (fn [] (println "Add "))))
                  table)]
      ["items"  (let [table (ui/table)]
                  (doseq [item (:items props)]
                    (.add table (ui/image (TextureRegionDrawable. (:texture (:image (get items/items item))))))
                    (.add table (ui/text-button " - " (fn [] (println "Remove " item))))
                    (.row table))
                  (.add table (ui/text-button " + " (fn [] (println "Add item"))))
                  table)]])))

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
  (let [table (ui/table)
        number-columns 20]
    (add-rows table [[[(ui/label title) :colspan number-columns]]])
    (add-rows table (for [entities (partition-all number-columns entities)]
                      (for [props entities
                            :let [button (ui/image-button (:image props)
                                                          #(.addActor (stage) (entity-editor (:id props))))
                                  ^Actor top-widget (extra-infos-widget props)
                                  stack (ui/stack)] ]
                        (do (ui/set-touchable top-widget :disabled)
                            (.add stack button)
                            (.add stack top-widget)
                            stack))))
    table))

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
  (let [table (ui/table)]
    (.add table (ui/text-button "Creatures" #(.setSecondWidget (split-pane) (creatures-table)))) (.row table)
    (.add table (ui/text-button "Items"     #(.setSecondWidget (split-pane) (items-table))))     (.row table)
    (.add table (ui/text-button "Skills"    #(.setSecondWidget (split-pane) (skills-table))))    (.row table)
    (.add table (ui/text-button "Back to Main Menu" #(app/set-screen :game.screens.main)))
    table))

(defmodule {:keys [stage]}
  (lc/create [_]
    (let [stage (ui/stage)
          table (ui/table)]
      (.setFillParent table true)
      (.setDebug table true)
      (.addActor stage table)
      (let [split-pane (ui/split-pane (left-widget)
                                      (ui/label "second widget")
                                      false)]
        (.setUserObject split-pane :split-pane)
        (.add table split-pane)
        {:stage stage
         :split-pane split-pane})))
  (lc/dispose [_] (.dispose stage))
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_] (gui/render #(ui/draw-stage stage)))
  (lc/tick [_ delta] (ui/update-stage stage delta)))
