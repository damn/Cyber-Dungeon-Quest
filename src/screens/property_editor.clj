(ns screens.property-editor
  (:require [clojure.edn :as edn]
            [gdl.app :refer [change-screen!]]
            [gdl.context :refer [get-stage ->text-button ->image-button ->label ->text-field
                                 ->image-widget ->table ->stack ->window]]
            [gdl.scene2d.actor :as actor :refer [remove! set-touchable! parent]]
            [gdl.scene2d.group :refer [add-actor! clear-children! children]]
            [gdl.scene2d.ui.table :refer [add! add-rows cells]]
            [gdl.scene2d.ui.cell :refer [set-actor!]]
            [gdl.scene2d.ui.widget-group :refer [pack!]]
            context.properties
            [cdq.context :refer [get-property all-properties]]))

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

(defn- add-to-stage! [ctx actor]
  (-> ctx get-stage (add-actor! actor)))

(declare ->property-editor-window)

(defn- open-property-editor-window! [context property-id]
  (add-to-stage! context (->property-editor-window context property-id)))

(defn- get-child-with-id [group id]
  (->> (children group)
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
                         :extra-infos-widget #(->label %1 (or (str (:level %2) "-")))}}
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

(defmethod property-widget :default [ctx _ v]
  (->label ctx (pr-str v))) ; TODO print-level set to nil ! not showing all effect -> print-fn?

(defmethod widget-data :default [_ _]
  nil)

(defmethod property-widget :image [ctx _ image]
  (->image-widget ctx image {}))

(defmethod property-widget :text-field [ctx _ v]
  (->text-field ctx (pr-str v) {}))

(defmethod widget-data :text-field [_ widget]
  (edn/read-string
   (.getText ^com.kotcrab.vis.ui.widget.VisTextField widget)))

(defmethod property-widget :link-button [context _ id]
  (->text-button context
                 (name id)
                 #(open-property-editor-window! % id)))

(defn- ->overview-table [context property-type clicked-id-fn]
  (let [{:keys [title
                sort-by-fn
                extra-infos-widget]} (:overview (get property-types property-type))
        entities (all-properties context property-type)
        entities (if sort-by-fn
                   (sort-by sort-by-fn entities)
                   entities)
        number-columns 20]
    (->table context
             {:rows (concat [[{:actor (->label context title) :colspan number-columns}]]
                            (for [entities (partition-all number-columns entities)]
                              (for [{:keys [id] :as props} entities
                                    :let [on-clicked #(clicked-id-fn % id)
                                          button (if (:image props)
                                                   (->image-button context (:image props) on-clicked)
                                                   (->text-button context (name id) on-clicked))
                                          top-widget (or (and extra-infos-widget
                                                              (extra-infos-widget context props))
                                                         (->label context ""))]]
                                (do
                                 (set-touchable! top-widget :disabled)
                                 (->stack context [button top-widget])))))})))

(defn- add-one-to-many-rows [context ^com.kotcrab.vis.ui.widget.VisTable table property-type property-ids]
  (.addSeparator table)
  (let [redo-rows (fn [context property-ids]
                    (clear-children! table)
                    (add-one-to-many-rows context table property-type property-ids))]
    (add-rows table (concat
                     (for [prop-id property-ids]
                       [(->image-widget context
                                        (:image (get-property context prop-id))
                                        {:id prop-id})
                        (->text-button context
                                       " - "
                                       #(redo-rows % (disj (set property-ids) prop-id)))])
                     [[(->text-button context
                                      " + "
                                      (fn [context]
                                        (let [window (->window context {:title "Choose"
                                                                        :modal? true
                                                                        :close-button? true
                                                                        :center? true
                                                                        ;:pack? true
                                                                        })
                                              clicked-id-fn (fn [context id]
                                                              (remove! window)
                                                              (redo-rows context
                                                                         (conj (set property-ids) id)))]
                                          (add! window (->overview-table context property-type clicked-id-fn))
                                          (pack! window)
                                          (add-to-stage! context window))))]])))
  (when-let [parent (parent table)]
    (pack! parent)))

(defmethod property-widget :one-to-many [context attribute property-ids]
  (let [table (->table context {})]
    (add-one-to-many-rows context table (attribute->property-type attribute) property-ids)
    table))

(defmethod widget-data :one-to-many [_ widget]
  (->> (.getChildren ^com.badlogic.gdx.scenes.scene2d.Group widget)
       (keep actor/id)
       set))

(defn- ->property-editor-window [context id]
  (let [props (get-property context id)
        {:keys [title property-keys]} (get property-types (context.properties/property-type props))
        window (->window context {:title title
                                  :modal? true
                                  :close-button? true
                                  :center? true
                                  :cell-defaults {:pad 5}})
        get-data #(into {}
                        (for [k property-keys
                              :let [widget (get-child-with-id window k)]]
                          [k (or (widget-data k widget)
                                 (get props k))]))]
    (add-rows window (concat (for [k property-keys
                                   :let [widget (property-widget context k (get props k))]]
                               (do
                                (actor/set-id! widget k)
                                [(->label context (name k)) widget]))
                             ; TODO doesnt do anything, need to swap on current context
                             [[(->text-button context "Save" #(context.properties/update-and-write-to-file! % (get-data)))
                               (->text-button context "Cancel" (fn [_context] (remove! window)))]]))
    (pack! window)
    window))

(defn- set-second-widget! [context widget]
  (let [table (:main-table (get-stage context))]
    (set-actor! (second (cells table)) widget)
    (pack! table)))

(defn- ->left-widget [context]
  (->table context {:cell-defaults {:pad 5}
                    :rows (concat
                           (for [[property-type {:keys [overview]}] (select-keys property-types [:creature :item :skill :weapon])]
                             [(->text-button context
                                             (:title overview)
                                             #(set-second-widget! % (->overview-table % property-type open-property-editor-window!)))])
                           [[(->text-button context "Back to Main Menu" (fn [_context]
                                                                          (change-screen! :screens/main-menu)))]])}))

(defn screen [context]
  {:actors [(->table context {:id :main-table
                              :rows [[(->left-widget context) nil]]
                              :fill-parent? true})]})
