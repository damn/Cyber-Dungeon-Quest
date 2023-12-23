(ns screens.property-editor
  (:require [clojure.edn :as edn]
            [gdl.app :refer [change-screen!]]
            [gdl.context :refer [get-stage ->text-button ->image-button]]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ui :as ui]
            [game.context :refer [get-property all-properties]]
            context.properties)
  (:import com.badlogic.gdx.scenes.scene2d.Stage
           (com.badlogic.gdx.scenes.scene2d.ui WidgetGroup)))

(comment
 (get-property @current-context :dragon-shadow)

 ; FIXME could not save dragon level input because the key was not in data
 ; so he complains at overwrite and save...

 ; TODO schema !
 ; cannot  save spider as it doesnt have :level ! ...

 (def win (first (filter #(instance? com.kotcrab.vis.ui.widget.VisWindow %) (.getActors (stage)))))

 (.layout (get-child-with-id win :skills))

 (.pack (.getParent (get-child-with-id win :skills)))

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

; Idea;
; during running game each entity has property/id
; can right click and edit the properties on the fly of _everything_
; in non-debug mode only presenting, otherwise editable.

; * validation on load all properties / save property/properties
; => spec => key spec
; * items => skill & item both , duplicate pretty-name & image hmmm ....
; or special case for weapon?
; * also item needs to be in certain slot, each slot only once, etc. also max-items ...?
; * non-toggle ->image-button at overview => VisImageButton
; * missing widgets for keys / one-to-many not implemented

(declare property-editor-window) ; ->property-editor-window or ->property-window

(defn- open-property-editor-window [{:keys [gui-viewport-width
                                            gui-viewport-height] :as context}
                                    property-id]
  (let [window (property-editor-window context property-id)]
    (.addActor ^Stage (get-stage context) window)
    (actor/set-center window
                      (/ gui-viewport-width  2)
                      (/ gui-viewport-height 2))))

(defn- get-child-with-id [group id]
  (->> (.getChildren ^com.badlogic.gdx.scenes.scene2d.Group group)
       (filter #(= id (actor/id %)))
       first))

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

(defn- attribute->property-type [k]
  (case k
    :skills :skill
    :items :item))

(def ^:private attribute-widget
  {:id :label
   :image :image
   :level :text-field
   :species :link-button
   :hp :text-field
   :speed :text-field
   :skills :one-to-many
   :items :one-to-many})

; property-key->create-widget
(defmulti property-widget (fn [_context k _v] (get attribute-widget k)))

; widget->data
(defmulti widget-data (fn [k _widget] (get attribute-widget k)))

(defmethod property-widget :default [_context _ v]
  (ui/label (pr-str v))) ; TODO print-level set to nil ! not showing all effect -> print-fn?

(defmethod widget-data :default [_ _]
  nil)

(defmethod property-widget :image [_context _ image]
  (ui/image (ui/texture-region-drawable (:texture image))))

(defmethod property-widget :text-field [_context _ v]
  (ui/text-field (pr-str v)))

(defmethod widget-data :text-field [_ widget]
  (edn/read-string
   (.getText ^com.kotcrab.vis.ui.widget.VisTextField widget)))

(defmethod property-widget :link-button [context _ id]
  (->text-button context
                 (name id)
                 #(open-property-editor-window % id)))

(defn- overview-table ^com.badlogic.gdx.scenes.scene2d.ui.Table [context property-type clicked-id-fn]
  (let [{:keys [title
                sort-by-fn
                extra-infos-widget]} (:overview (get property-types property-type))
        entities (all-properties context property-type)
        entities (if sort-by-fn
                   (sort-by sort-by-fn entities)
                   entities)
        number-columns 20]
    (ui/table :rows (concat [[{:actor (ui/label title) :colspan number-columns}]]
                            (for [entities (partition-all number-columns entities)]
                              (for [{:keys [id] :as props} entities
                                    :let [on-clicked #(clicked-id-fn % id)
                                          button (if (:image props)
                                                   (->image-button context (:image props) on-clicked)
                                                   (->text-button context (name id) on-clicked))
                                          top-widget (or (and extra-infos-widget
                                                              (extra-infos-widget props))
                                                         (ui/label ""))
                                          stack (ui/stack)]]
                                (do (actor/set-touchable top-widget :disabled)
                                    (.add stack button)
                                    (.add stack top-widget)
                                    stack)))))))

(defn- add-one-to-many-rows [context ^com.kotcrab.vis.ui.widget.VisTable table property-type property-ids]
  (.addSeparator table)
  (let [redo-rows (fn [context property-ids]
                    (.clearChildren ^com.badlogic.gdx.scenes.scene2d.ui.Table table)
                    (add-one-to-many-rows context table property-type property-ids))]
    (ui/add-rows table (concat
                        (for [prop-id property-ids]
                          [(ui/image (ui/texture-region-drawable (:texture (:image (get-property context prop-id))))
                                     :id prop-id)
                           (->text-button context
                                          " - "
                                          #(redo-rows % (disj (set property-ids) prop-id)))])
                        [[(->text-button context
                                         " + "
                                         (fn [{:keys [gui-viewport-width gui-viewport-height]}]
                                           (let [window (ui/window :title "Choose"
                                                                   :modal? true)
                                                 clicked-id-fn (fn [context id]
                                                                 (.remove window)
                                                                 (redo-rows context
                                                                            (conj (set property-ids) id)))]
                                             (.add window (overview-table context property-type clicked-id-fn))
                                             (.pack window)
                                             ; TODO fn above -> open in center .. ?
                                             (.addActor ^Stage (get-stage context) window)
                                             (actor/set-center window
                                                               (/ gui-viewport-width  2)
                                                               (/ gui-viewport-height 2)))))]])))
  (when-let [parent (.getParent table)]
    (.pack ^WidgetGroup parent)))

(defmethod property-widget :one-to-many [context attribute property-ids]
  (let [table (ui/table)]
    (add-one-to-many-rows context table (attribute->property-type attribute) property-ids)
    table))

(defmethod widget-data :one-to-many [_ widget]
  (->> (.getChildren ^com.badlogic.gdx.scenes.scene2d.Group widget)
       (keep actor/id)
       set))

(defn- property-editor-window [context id]
  (let [props (get-property context id)
        {:keys [title property-keys]} (get property-types (context.properties/property-type props))
        window (ui/window :title title
                          :modal? true
                          :cell-defaults {:pad 5})
        get-data #(into {}
                        (for [k property-keys
                              :let [widget (get-child-with-id window k)]]
                          [k (or (widget-data k widget)
                                 (get props k))]))]
    (ui/add-rows window (concat (for [k property-keys
                                      :let [widget (property-widget context k (get props k))]]
                                  (do
                                   (actor/set-id widget k)
                                   [(ui/label (name k)) widget]))
                                ; TODO doesnt do anything, need to swap on current context
                                [[(->text-button context "Save" #(context.properties/update-and-write-to-file! % (get-data)))
                                  (->text-button context "Cancel" (fn [_context] (.remove window)))]]))
    (.pack window)
    window))

(defn- set-second-widget [context widget]
  (let [^com.badlogic.gdx.scenes.scene2d.ui.Table table (:main-table (get-stage context))]
    (.setActor ^com.badlogic.gdx.scenes.scene2d.ui.Cell
               (second (.getCells table)) widget)
    (.pack table)))

(defn- left-widget [context]
  (ui/table :cell-defaults {:pad 5}
            :rows (concat
                   (for [[property-type {:keys [overview]}] (select-keys property-types [:creature :item :skill :weapon])]
                     [(->text-button context
                                     (:title overview)
                                     #(set-second-widget % (overview-table % property-type open-property-editor-window)))])
                   [[(->text-button context "Back to Main Menu" (fn [_context]
                                                                  (change-screen! :screens/main-menu)))]])))

(defn screen [context]
  {:actors [(ui/table :id :main-table
                      :rows [[(left-widget context) nil]]
                      :fill-parent? true)]})
