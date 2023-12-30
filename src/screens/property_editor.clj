(ns screens.property-editor
  (:require [clojure.edn :as edn]
            [gdl.app :refer [change-screen!]]
            [gdl.context :refer [get-stage ->text-button ->image-button ->label ->text-field
                                 ->image-widget ->table ->stack ->window ->text-tooltip]]
            [gdl.scene2d.actor :as actor :refer [remove! set-touchable! parent add-listener!]]
            [gdl.scene2d.group :refer [add-actor! clear-children! children]]
            [gdl.scene2d.ui.text-field :as text-field]
            [gdl.scene2d.ui.table :refer [add! add-rows cells add-separator!]]
            [gdl.scene2d.ui.cell :refer [set-actor!]]
            [gdl.scene2d.ui.widget-group :refer [pack!]]
            context.properties
            [cdq.context :refer [get-property all-properties]]))

; ADD SOUNDS TO SPELLS MANDATORY - move out of restoration - also hp / mana separate
; LET ME EDIT for example spawn which creature
; => SCHEMA

; TODO refresh overview table after property-editor save something (callback ?)
; remove species, directly hp/speed ( no multiplier )

(defn- add-to-stage! [ctx actor]
  (-> ctx get-stage (add-actor! actor)))

(declare ->property-editor-window)

(defn- open-property-editor-window! [context property-id]
  (add-to-stage! context (->property-editor-window context property-id)))

(defn- default-property-tooltip-text [context props]
  (binding [*print-level* nil]
    (with-out-str
     (clojure.pprint/pprint (dissoc props :image)))))

(def ^:private property-types
  {:species {:title "Species"
             :property-keys [:id :hp :speed]
             :overview {:title "Species"}}
   :creature {:title "Creature"
              :property-keys [:id :image :species :level :skills :items]
              :overview {:title "Creatures"
                         :sort-by-fn #(vector (or (:level %) 9) (name (:species %)) (name (:id %)))
                         :extra-infos-widget #(->label %1 (or (str (:level %2)) "-"))
                         :tooltip-text-fn default-property-tooltip-text}}
   :item {:title "Item"
          :property-keys [:id :image :slot :pretty-name :modifier]
          :overview {:title "Items"
                     :sort-by-fn #(vector (if-let [slot (:slot %)] (name slot) "") (name (:id %)))
                     :tooltip-text-fn default-property-tooltip-text}}
   :skill {:title "Spell"
           :property-keys [:id :image :action-time :cooldown :cost :effect]
           :overview {:title "Spells"
                      :tooltip-text-fn (fn [ctx props]
                                         (try (cdq.context/skill-text ctx props)
                                              (catch Throwable t
                                                (default-property-tooltip-text ctx props))))}}
   :weapon {:title "Weapon"
            :property-keys [:id :image :pretty-name :action-time :effect]
            :overview {:title "Weapons"
                       :tooltip-text-fn (fn [ctx props]
                                          (try (cdq.context/skill-text ctx props)
                                              (catch Throwable t
                                                (default-property-tooltip-text ctx props))))}}})

;;

(defn- one-to-many-attribute->linked-property-type [k]
  (case k
    :skills :skill
    :items :item))

(def ^:private attribute->attribute-widget
  {:id :label
   :image :image
   :level :text-field
   :species :link-button
   :hp :text-field
   :speed :text-field
   :skills :one-to-many
   :items :one-to-many
   :effect :nested-map
   :effect/sound :sound})

;;

(defmulti ->attribute-widget
  (fn [_context [k _v]]
    (get attribute->attribute-widget k)))

(defmethod ->attribute-widget :default [ctx [_k v]]
  (->label ctx (pr-str v))) ; TODO print-level set to nil ! not showing all effect -> print-fn?

(defmulti attribute-widget->data
  (fn [_widget [k _v]]
    (get attribute->attribute-widget k)))

(defmethod attribute-widget->data :default [_widget _kv]
  nil)

;;

(defmethod ->attribute-widget :image [ctx [_ image]]
  (->image-widget ctx image {}))

;;

(defmethod ->attribute-widget :text-field [ctx [_ v]]
  (->text-field ctx (pr-str v) {}))

(defmethod attribute-widget->data :text-field [widget _kv]
  (edn/read-string (text-field/text widget)))

;;

(defmethod ->attribute-widget :link-button [context [_ prop-id]]
  (->text-button context (name prop-id) #(open-property-editor-window! % prop-id)))

;;

(declare ->attribute-widgets
         attribute-widgets->all-data)

(defmethod ->attribute-widget :nested-map [ctx [_k props]]
  (->table ctx {:cell-defaults {:pad 5}
                :rows (->attribute-widgets ctx props)}))

(defmethod attribute-widget->data :nested-map [widget [_k props]]
  (attribute-widgets->all-data widget props))

;;

(defmethod ->attribute-widget :sound [ctx [_ sound-file]]
  (->text-button ctx (name sound-file) #(gdl.context/play-sound! % sound-file)))

(defmethod attribute-widget->data :sound [widget _]
  nil)

;;

(defn- ->overview-table
  "Creates a table with all-properties of property-type and buttons for each id
  which on-clicked calls clicked-id-fn."
  [ctx property-type clicked-id-fn]
  (let [{:keys [title
                sort-by-fn
                extra-infos-widget
                tooltip-text-fn]} (:overview (get property-types property-type))
        entities (all-properties ctx property-type)
        entities (if sort-by-fn
                   (sort-by sort-by-fn entities)
                   entities)
        number-columns 20]
    (->table ctx
             {:cell-defaults {:pad 2}
              :rows (concat [[{:actor (->label ctx title) :colspan number-columns}]]
                            (for [entities (partition-all number-columns entities)]
                              (for [{:keys [id] :as props} entities
                                    :let [on-clicked #(clicked-id-fn % id)
                                          button (if (:image props)
                                                   (->image-button ctx (:image props) on-clicked)
                                                   (->text-button ctx (name id) on-clicked))
                                          top-widget (or (and extra-infos-widget
                                                              (extra-infos-widget ctx props))
                                                         (->label ctx ""))
                                          stack (->stack ctx [button top-widget])]]
                                (do
                                 (when tooltip-text-fn
                                   (add-listener! button (->text-tooltip ctx #(tooltip-text-fn % props))))
                                 (set-touchable! top-widget :disabled)
                                 stack))))})))

(defn- add-one-to-many-rows [ctx table property-type property-ids]
  (add-separator! table)
  (let [redo-rows (fn [ctx property-ids]
                    (clear-children! table)
                    (add-one-to-many-rows ctx table property-type property-ids))]
    (add-rows table (concat
                     (for [prop-id property-ids]
                       [(let [props (get-property ctx prop-id)
                              image-widget (->image-widget ctx
                                                           (:image props)
                                                           {:id (:id props)})]
                          (add-listener! image-widget (->text-tooltip ctx #((-> property-types
                                                                                property-type
                                                                                :overview
                                                                                :tooltip-text-fn) % props)))
                          image-widget)
                        (->text-button ctx
                                       " - "
                                       #(redo-rows % (disj (set property-ids) prop-id)))])
                     [[(->text-button ctx
                                      " + "
                                      (fn [ctx]
                                        (let [window (->window ctx {:title "Choose"
                                                                    :modal? true
                                                                    :close-button? true
                                                                    :center? true
                                                                    :close-on-escape? true})
                                              clicked-id-fn (fn [ctx id]
                                                              (remove! window)
                                                              (redo-rows ctx
                                                                         (conj (set property-ids) id)))]
                                          (add! window (->overview-table ctx property-type clicked-id-fn))
                                          (pack! window)
                                          (add-to-stage! ctx window))))]])))
  (when-let [parent (parent table)]
    (pack! parent)))

(defmethod ->attribute-widget :one-to-many [context [attribute property-ids]]
  (let [table (->table context {})]
    (add-one-to-many-rows context
                          table
                          (one-to-many-attribute->linked-property-type attribute)
                          property-ids)
    table))

(defmethod attribute-widget->data :one-to-many [widget _]
  (->> (children widget) (keep actor/id) set))

;;

(defn- ->attribute-widgets [ctx props]
  (for [[k v] props
        :let [widget (->attribute-widget ctx [k v])]]
    (do
     (actor/set-id! widget k)
     [(->label ctx (name k)) widget])))

(defn- attribute-widgets->all-data [parent props]
  (into {}
        (for [[k v] props
              :let [widget (k parent)]]
          [k (or (attribute-widget->data widget [k v])
                 v)])))

;;

(defn- ->property-editor-window [context id]
  (let [props (get-property context id)
        {:keys [title property-keys]} (get property-types (context.properties/property-type props))
        window (->window context {:title title
                                  :modal? true
                                  :close-button? true
                                  :center? true
                                  :close-on-escape? true
                                  :cell-defaults {:pad 5}})
        widgets (->attribute-widgets context props)
        get-data #(attribute-widgets->all-data window props)]
    (add-rows window (concat widgets
                             [[(->text-button context "Save"
                                              (fn [_ctx]
                                                ; TODO error modal like map editor?
                                                ; TODO redo/close overview ?
                                                (swap! gdl.app/current-context
                                                       context.properties/update-and-write-to-file! (get-data))
                                                (remove! window)))
                               (->text-button context "Cancel" (fn [_ctx]
                                                                 (remove! window)))]]))
    (pack! window)
    window))

;;

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

(defn screen [context background-image-fn]
  {:actors [(background-image-fn)
            (->table context {:id :main-table
                              :rows [[(->left-widget context) nil]]
                              :fill-parent? true})]})
