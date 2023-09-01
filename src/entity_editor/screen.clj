(ns entity-editor.screen
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.app :as app]
            [gdl.input :as input]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.batch :refer [batch]]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui :as ui]
            [game.properties :as properties]))

; TODO stage dispose necessary everywhere ??

(defn- stage []
  (:stage (app/current-screen-value)))

(defn- split-pane []
  (:split-pane (app/current-screen-value)))

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


; TODO for each key of entity-type
; create for [k v] textfield with :id ...
; and findActor and getText

; what about undo? validation? etc. a label if changes? cancel warn on unsaved changes, etc. ?

(defmulti property-widget (fn [k v] k))

(defmethod property-widget :default [_ v]
  (ui/text-field (pr-str v)))

(defmethod property-widget :id [_ id] ; => not-editable => label. (also :slot)
  (ui/label (pr-str id)))

(defmethod property-widget :image [_ image]
  (ui/image (ui/texture-region-drawable (:texture image))))

(declare species-editor)

; == > link to another
(defmethod property-widget :species [_ species-id]
  (ui/text-button (name species-id)
                  #(stage/add-actor (stage) (species-editor species-id))))

; :id =>
; setDisabled true

(comment

 :items / :skills ; ==> link to multiple others
 ; also item needs to be in certain slot, each slot only once, etc. also max-items ...?
 (ui/table :rows (concat
                  (for [skill (:skills props)]
                    [(ui/image (ui/texture-region-drawable (:texture (:image (properties/get skill)))))
                     (ui/text-button " - " (fn [] (println "Remove " )))])
                  [[(ui/text-button " + " (fn [] (println "Add ")))]]))
 )

(defn- editor-window [& {:keys [title id property-keys]}]
  (let [window (ui/window :title title
                          :modal? true
                          :cell-defaults {:pad 5})
        props (properties/get id)]
    (ui/add-rows window (concat (for [k property-keys]
                                  [(ui/label (name k)) (property-widget k (get props k))])
                                [[(ui/text-button "Save" (fn [] ))
                                  (ui/text-button "Cancel" #(actor/remove window))]]))
    (ui/pack window)
    (actor/set-center window
                      (/ (gui/viewport-width)  2)
                      (/ (gui/viewport-height) 2))
    window))

(defn species-editor [species-id]
  (editor-window :title "Species"
                 :id species-id
                 :property-keys [:id :hp :speed]))

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


(defn- creature-editor [id]
  (editor-window :title "Creature"
                 :id id
                 :property-keys [:id :image :species :level :skills :items]))

; entity-type 'item'
; => edit entity with id
; => where to read properties?
; items => items/items
; 'image-widget' => :image property

(comment
 ; entity type
 ; has keys/attributes.
 ; e.g. id/image/this/that
 ; for each attribute have edit-widget,editable or not,etc.
 ; e.g. item => id,image,slot,pretty-name,modifiers
 )

(defn- item-editor [id]
  (editor-window :title "Item"
                 :id id
                 :property-keys [:id :image :slot :pretty-name :modifiers]))
; TODO crashes when no slot of item (unimplemented items, e.g. empty heart ) !
; => schema on edn/read validate and on save !!

; TODO effect, first choose from list which one
; then edit properties (different for each effect => depends on params !)
; param types ... e.g. damage.

(defn- skill-editor [id]
  (editor-window :title "Skill"
                 :id id
                 :property-keys [:id :image :action-time :cooldown :cost :effect]))

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
                                                                  ; TODO same code @ species link ...
                                                                  #(stage/add-actor (stage) (entity-editor (:id props))))
                                          top-widget (extra-infos-widget props)
                                          stack (ui/stack)] ]
                                (do (actor/set-touchable top-widget :disabled)
                                    (.add stack button)
                                    (.add stack top-widget)
                                    stack)))))))

(defn- creatures-table []
  (entities-table :title "Creatures"
                  :entities (sort-by #(vector (or (:level %) 9)
                                              (name (:species %)))
                                     (properties/all-with-key :species))
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
                                     (properties/all-with-key :slot))
                  :entity-editor item-editor
                  :extra-infos-widget (fn [_] (ui/label ""))))

(defn- skills-table []
  (entities-table :title "Skills"
                  :entities (properties/all-with-key :spell?)
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
    (let [stage (stage/create gui/viewport batch)
          split-pane (ui/split-pane :first-widget (left-widget)
                                    :second-widget (creatures-table)
                                    :vertical? false
                                    :id :split-pane)
          table (ui/table :rows [[split-pane]]
                          :fill-parent? true)]
      (stage/add-actor stage table)
      {:stage stage
       :split-pane split-pane})) ; TODO only stage needed, can get split-pane through table
  (lc/dispose [_] (.dispose stage))
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_] (gui/render #(stage/draw stage batch)))
  (lc/tick [_ delta] (stage/act stage delta)))
