(ns property-editor.screen
  (:require [clojure.edn :as edn]
            [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.app :as app]
            [gdl.input :as input]
            [gdl.utils :refer [dispose]]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.batch :refer [batch]]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui :as ui]
            [game.properties :as properties]))

; TODO:

; weapons are both items & skills ... how to edit ? or extra category ?
; => not assoc data but update data merge new-data
; => and keys is just minimum keys check...

; * validation on load all properties / save property/properties
; => spec => key spec
; * items => skill & item both , duplicate pretty-name & image hmmm ....
; or special case for weapon?
; * also item needs to be in certain slot, each slot only once, etc. also max-items ...?
; * non-toggle image-button at overview => VisImageButton
; * missing widgets for keys / one-to-many not implemented

(def ^:private stage app/current-screen-value)

(declare property-editor-window)

(defn- open-property-editor-window [id]
  (let [window (property-editor-window id)]
    (stage/add-actor (stage) window)
    (actor/set-center window
                      (/ (gui/viewport-width)  2)
                      (/ (gui/viewport-height) 2))))

(defn- get-child-with-id [group id]
  (->> (.getChildren ^com.badlogic.gdx.scenes.scene2d.Group group)
       (filter #(= id (actor/id %)))
       first))

; TODO and here
(def ^:private property-types
  {:species {:title "Species"
             :property-keys [:id :hp :speed]
             :overview {:title "Species"}}
   :creature {:title "Creature"
              :property-keys [:id :image :species :level :skills :items]
              :overview {:title "Creatures"
                         :sort-by-fn #(vector (or (:level %) 9) (name (:species %)) (name (:id %)))
                         :extra-infos-widget #(ui/label (or (str (:level %) "-")))}}
   :item {:title "Item"
          :property-keys [:id :image :slot :pretty-name :modifiers]
          :overview {:title "Items"
                     :sort-by-fn #(vector (if-let [slot (:slot %)] (name slot) "") (name (:id %)))}}
   :skill {:title "Skill"
           :property-keys [:id :image :action-time :cooldown :cost :effect]
           :overview {:title "Skills"}}
   :weapon {:title "Weapon"
            :property-keys [:id :image :pretty-name :action-time :effect]
            :overview {:title "Weapons"}}})

(def ^:private attribute-widget
  {:id :label
   :image :image
   :level :text-field
   :species :link-button
   :hp :text-field
   :speed :text-field
   :skills :one-to-many
   :items :one-to-many})

(defmulti property-widget (fn [k v] (get attribute-widget k)))
(defmulti widget-data (fn [k widget] (get attribute-widget k)))

(defmethod property-widget :default [_ v]
  (ui/label (pr-str v))) ; TODO print-level set to nil ! not showing all effect -> print-fn?

(defmethod widget-data :default [_ _]
  nil)

(defmethod property-widget :image [_ image]
  (ui/image (ui/texture-region-drawable (:texture image))))

(defmethod property-widget :text-field [_ v]
  (ui/text-field (pr-str v)))

(defmethod widget-data :text-field [_ widget]
  (edn/read-string
   (.getText ^com.kotcrab.vis.ui.widget.VisTextField widget)))

(defmethod property-widget :link-button [_ id]
  (ui/text-button (name id) #(open-property-editor-window id)))

(defn- add-one-to-many-rows [table property-ids]
  (.addSeparator ^com.kotcrab.vis.ui.widget.VisTable table)
  (ui/add-rows table (concat
                      (for [prop-id property-ids]
                        [(ui/image (ui/texture-region-drawable (:texture (:image (properties/get prop-id))))
                                   :id prop-id)
                         (ui/text-button " - " (fn []
                                                 (.clearChildren ^com.badlogic.gdx.scenes.scene2d.ui.Table table)
                                                 (add-one-to-many-rows table (disj (set property-ids) prop-id))))])
                      [[(ui/text-button " + " (fn [] (println "Add ")))]])))

(defmethod property-widget :one-to-many [_ property-ids]
  (let [table (ui/table)]
    (add-one-to-many-rows table property-ids)
    table))

(defmethod widget-data :one-to-many [_ widget]
  (->> (.getChildren ^com.badlogic.gdx.scenes.scene2d.Group widget)
       (keep actor/id)
       set))

(comment

 ; TODO schema !
 ; cannot  save spider as it doesnt have :level ! ...


 (let [window (first (filter #(instance? com.kotcrab.vis.ui.widget.VisWindow %) (.getActors (stage))))
       props (properties/get :wizard)
       new-props (into {}
                       (for [k (:property-keys (get property-types :creature))
                             :let [widget (get-child-with-id window k)]]
                         [k (or (widget-data k widget)
                                (get props k))]))
       ]
   (= (set (keys props))
      (set (keys new-props)))

   )
 )

(defn- property-editor-window [id]
  (let [props (properties/get id)
        {:keys [title property-keys]} (get property-types (properties/property-type props))
        window (ui/window :title title
                          :modal? true
                          :cell-defaults {:pad 5})
        get-data #(into {}
                        (for [k property-keys
                              :let [widget (get-child-with-id window k)]]
                          [k (or (widget-data k widget)
                                 (get props k))]))]
    (ui/add-rows window (concat (for [k property-keys
                                      :let [widget (property-widget k (get props k))]]
                                  (do
                                   (actor/set-id widget k)
                                   [(ui/label (name k)) widget]))
                                [[(ui/text-button "Save" #(properties/save! (get-data)))
                                  (ui/text-button "Cancel" #(actor/remove window))]]))
    (ui/pack window)
    window))

(defn- overview-table [property-type]
  (let [{:keys [title
                sort-by-fn
                extra-infos-widget]} (:overview (get property-types property-type))
        entities (properties/get-all property-type)
        entities (if sort-by-fn
                   (sort-by sort-by-fn entities)
                   entities)
        number-columns 20]
    (ui/table :rows (concat [[{:actor (ui/label title) :colspan number-columns}]]
                            (for [entities (partition-all number-columns entities)]
                              (for [{:keys [id] :as props} entities
                                    :let [open-editor-fn #(open-property-editor-window id)
                                          button (if (:image props)
                                                   (ui/image-button (:image props) open-editor-fn)
                                                   (ui/text-button (name id) open-editor-fn))
                                          top-widget (or (and extra-infos-widget
                                                              (extra-infos-widget props))
                                                         (ui/label ""))
                                          stack (ui/stack)]]
                                (do (actor/set-touchable top-widget :disabled)
                                    (.add stack button)
                                    (.add stack top-widget)
                                    stack)))))))

(defn- set-second-widget [widget]
  (let [^com.badlogic.gdx.scenes.scene2d.ui.Table table (:main-table (stage))]
    (.setActor ^com.badlogic.gdx.scenes.scene2d.ui.Cell
               (second (.getCells table)) widget)
    (.pack table)))

(defn- left-widget []
  (ui/table :cell-defaults {:pad 5}
            :rows (concat
                   ; TODO and here
                   (for [[property-type {:keys [overview]}] (select-keys property-types [:creature :item :skill :weapon])]
                     [(ui/text-button (:title overview) #(set-second-widget (overview-table property-type)))])
                   [[(ui/text-button "Back to Main Menu" #(app/set-screen :game.screens.main))]])))

(defmodule stage
  (lc/create [_]
    (let [stage (stage/create gui/viewport batch)
          table (ui/table :id :main-table
                          :rows [[(left-widget) nil]]
                          :fill-parent? true)]
      (stage/add-actor stage table)
      stage))
  (lc/dispose [_] (dispose stage))
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_] (gui/render #(stage/draw stage batch)))
  (lc/tick [_ delta] (stage/act stage delta)))
