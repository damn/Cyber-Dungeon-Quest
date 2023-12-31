(ns screens.property-editor
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [gdl.app :as app :refer [change-screen!]]
            [gdl.context :refer [get-stage ->text-button ->image-button ->label ->text-field ->image-widget ->table ->stack ->window ->text-tooltip all-sound-files play-sound!]]
            [gdl.scene2d.actor :as actor :refer [remove! set-touchable! parent add-listener!]]
            [gdl.scene2d.group :refer [add-actor! clear-children! children]]
            [gdl.scene2d.ui.text-field :as text-field]
            [gdl.scene2d.ui.table :refer [add! add-rows cells add-separator!]]
            [gdl.scene2d.ui.cell :refer [set-actor!]]
            [gdl.scene2d.ui.widget-group :refer [pack!]]
            [context.properties :as properties]
            [cdq.context :refer [get-property all-properties]]))

; TODO all properties which have no property type -> misc -> select from scroll pane text buttons
; e.g. first-level .... want to edit ! or @ map editor ?

; ADD SOUNDS TO SPELLS MANDATORY - move out of restoration - also hp / mana separate
; LET ME EDIT for example spawn which creature
; => SCHEMA

; => let me ADD effect components like sound & SELECT sound ! file chooser
; do it properly right do it with your editor not text ....

; TODO refresh overview table after property-editor save something (callback ?)
; remove species, directly hp/speed ( no multiplier )

; used @ player-modal / error-window , can move to gdl.context
(defn- add-to-stage! [ctx actor]
  (-> ctx get-stage (add-actor! actor))
  (.setScrollFocus (get-stage ctx) actor) ; TODO not working
  (.setKeyboardFocus (get-stage ctx) actor)
  )

(defn- default-property-tooltip-text [context props]
  (binding [*print-level* nil]
    (with-out-str
     (clojure.pprint/pprint (dissoc props :image)))))

; property-keys not used
; but schema? add/remove something ? or prepare already in resources/properties.edn
; => still schema for checking - all items,spells,etc. ok?

; creature
; item
; spell  = skill type
; weapon = skill type & item also or skill modifier myself?

; weapon is just item with yourself as skill modifier ... but its BOTH
; so how to do text ? based on all keys always ?

(comment
 {:property/id :staff,
  ; property/image ?? used @ skill & item ...
  ; => will become :entity/image then ... make namespaced so easier to find ...
  :property/image {:file "items/images.png", :sub-image-bounds [528 144 48 48]},
  :property/pretty-name "Staff",
  :item/slot :weapon,
  :item/two-handed? true,
  :skill/action-time 0.5,
  :skill/effect
  [[:effect/target-entity
    {:maxrange 0.6,
     :hit-effect [[:effect/damage [:physical [3 6]]]]}]]})

; or properties no type but components/composition
; e.g.
{:property/id :weapons/staff
 :skill {:foo :bar}
 :item {:bar :baz}
 ; ?
 }

(def ^:private property-types
  {:species {:title "Species"
             ;:property-keys [:id :hp :speed]
             :overview {:title "Species"}}
   :creature {:title "Creature"
              ;:property-keys [:id :image :species :level :skills :items]
              :overview {:title "Creatures"
                         :sort-by-fn #(vector (or (:level %) 9) (name (:species %)) (name (:id %)))
                         :extra-infos-widget #(->label %1 (or (str (:level %2)) "-"))
                         :tooltip-text-fn default-property-tooltip-text}}
   :item {:title "Item"
          ;:property-keys [:id :image :slot :pretty-name :modifier]
          :overview {:title "Items"
                     :sort-by-fn #(vector (if-let [slot (:slot %)] (name slot) "") (name (:id %)))
                     :tooltip-text-fn default-property-tooltip-text}}
   :skill {:title "Spell"
           ;:property-keys [:id :image :action-time :cooldown :cost :effect]
           :overview {:title "Spells"
                      :tooltip-text-fn (fn [ctx props]
                                         (try (cdq.context/skill-text ctx props)
                                              (catch Throwable t
                                                (default-property-tooltip-text ctx props))))}}
   :weapon {:title "Weapon"
            ;:property-keys [:id :image :pretty-name :action-time :effect]
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
   :slot :label
   :spell? :label
   :image :image
   :species :link-button
   :speed :text-field
   :skills :one-to-many
   :items :one-to-many
   :effect :nested-map
   :modifier :nested-map
   :effect/sound :sound})

(defn- sort-attributes [properties]
  (sort-by
   (fn [[k _v]]
     [(case k
        :id 0
        :image 1
        :pretty-name 2
        :level 3
        :slot 3
        :species 4
        9)
      (name k)])
   properties))

;;

(defmulti ->attribute-widget     (fn [_context [k _v]] (get attribute->attribute-widget k)))
(defmulti attribute-widget->data (fn [_widget  [k _v]] (get attribute->attribute-widget k)))

(defmethod ->attribute-widget :default [ctx [_ v]]
  (->text-field ctx (pr-str v) {}))

(defmethod attribute-widget->data :default [widget _kv]
  (edn/read-string (text-field/text widget)))

;;

(defmethod ->attribute-widget :label [ctx [_k v]]
  (->label ctx (pr-str v))) ; TODO print-level set to nil ! not showing all effect -> print-fn?

(defmethod attribute-widget->data :label [_widget _kv]
  nil)

;;

; TODO too many ! too big ! scroll ... only show files first & preview?
; TODO make tree view from folders, etc. .. !! all creatures animations showing...
(defn- texture-rows [ctx]
  (for [file (sort (gdl.context/all-texture-files ctx))]
    [(->image-button ctx
                      (gdl.context/create-image ctx file)
                      (fn [_ctx]))]
    #_[(->text-button ctx
                    file
                    (fn [_ctx]))]))

(defn ->scroll-pane [_ actor]
  (let [widget (com.kotcrab.vis.ui.widget.VisScrollPane. actor)]
    (.setFlickScroll widget false)
    (.setFadeScrollBars widget false)
    ; TODO set touch focus , have to click first to use scroll pad
    ; TODO use scrollpad ingame too
    widget))

(defn ->list-textures-window [ctx]
  (let [window  (->window ctx {:title "Choose"
                 :modal? true
                 :close-button? true
                 :center? true
                 :close-on-escape? true
                 ;:rows [[(->scroll-pane ctx (->table ctx {:rows (texture-rows ctx)}))]]
                 :pack? true})]

    ;(.add window (->scroll-pane ctx (->table ctx {:rows (texture-rows ctx)})))
    (.width
     (.height (.add window (->scroll-pane ctx (->table ctx {:rows (texture-rows ctx)})))
              (float (- (:gui-viewport-height ctx) 50)))
     (float 750)
     )
    ;(.fill (.add window (->scroll-pane ctx (->table ctx {:rows (texture-rows ctx)}))))
    (.pack window)
    ;(.setFillParent window true)
    window
    ))

;;

(defmethod ->attribute-widget :image [ctx [_ image]]
  (->image-button ctx image #(add-to-stage! % (->list-textures-window %))))

(defmethod attribute-widget->data :image [_widget _kv]
  nil)

;;

(declare ->property-editor-window)

(defn open-property-editor-window! [context property-id]
  (add-to-stage! context (->property-editor-window context property-id)))

(defmethod ->attribute-widget :link-button [context [_ prop-id]]
  (->text-button context (name prop-id) #(open-property-editor-window! % prop-id)))

(defmethod attribute-widget->data :link-button [_widget _kv]
  nil)

;;

; TODO add effect/modifier components
; => data schema for all modifiers/effects & value ranges?

; TODO what happens if you select damage or sound or whatever ?
; each has its own widget get added with default/empty value (cannot save then)
; => schema ! for each attribute.

; right click entity popup menu
; edit
; => open-property-editor-window! ingame !

(declare redo-nested-map-rows!)

(defn- nested-map-attribute->allowed-keys [k]
  (case k
    :modifier (keys context.modifier/modifier-definitions)
    :effect (keys (methods context.effect/do!))))

(declare ->attribute-widgets)

(defn- clicked-nested-map-overview [k ctx table window nested-k widget-rows]
  (remove! window)
  (redo-nested-map-rows! k ctx table
                         (conj widget-rows (first (->attribute-widgets ctx {nested-k nil})))))

(defn- ->nested-map-rows [k ctx table widget-rows]
  (conj (for [row widget-rows]
          (conj row
                (->text-button ctx "-" #(redo-nested-map-rows! k % table (disj widget-rows row)))))
        [{:actor (->text-button ctx (str "Add " (name k))
                                (fn [ctx]
                                  (let [window (->window ctx {:title "Choose"
                                                              :modal? true
                                                              :close-button? true
                                                              :center? true
                                                              :close-on-escape? true})]
                                    (add-rows window (for [nested-k (nested-map-attribute->allowed-keys k)]
                                                       [(->text-button ctx (name nested-k)
                                                                       #(clicked-nested-map-overview k % table window nested-k widget-rows))]))
                                    (pack! window)
                                    (add-to-stage! ctx window))))
          :colspan 3}]))

(defn- redo-nested-map-rows! [k ctx table widget-rows]
  (clear-children! table)
  (add-rows table (->nested-map-rows k ctx table widget-rows))
  (pack! (parent table)))

(defmethod ->attribute-widget :nested-map [ctx [k props]]
  (let [table (->table ctx {:cell-defaults {:pad 5}})]
    (add-rows table (->nested-map-rows k ctx table (set (->attribute-widgets ctx props))))
    table))

(declare attribute-widgets->all-data)

(defmethod attribute-widget->data :nested-map [table [_k props]]
  (let [data (attribute-widgets->all-data table props)]
    (println "get data :nested-map " data)
    data))

(comment
 (let [ctx @gdl.app/current-context
       table (:modifier (gdl.context/mouse-on-stage-actor? ctx))
       ]
   (attribute-widgets->all-data table #:modifier{:armor nil :max-hp nil :shield nil})
   ; need to get not props but from widgets themself the value ...
   ; a widget should hold its value by which it was created?

   )
 )

;;

; TODO select from all sounds (see duration, waveform, if already used somewhere ?)
; TODO selectable sound / pass widget->data somehow
; TODO do the same for image!!
; TODO -> modal window reuse code?

(defn- sound-rows [ctx]
  (for [sounds (partition-all 5 (all-sound-files ctx))]
    (for [sound sounds]
      (->text-button ctx
                     (str/replace-first sound "sounds/" "")
                     #(play-sound! % sound)))))

(defn ->list-sounds-window [ctx]
  (->window ctx {:title "Choose"
                 :modal? true
                 :close-button? true
                 :center? true
                 :close-on-escape? true
                 :rows (sound-rows ctx)
                 :pack? true}))

(defmethod ->attribute-widget :sound [ctx [_ sound-file]]
  (->table ctx {:cell-defaults {:pad 5}
                :rows [[(->text-button ctx
                                       (name sound-file)
                                       #(add-to-stage! % (->list-sounds-window %)))
                        (->text-button ctx
                                       "play"
                                       #(play-sound! % sound-file))]]}))

(defmethod attribute-widget->data :sound [widget _]
  nil) ; TODO needs to pass value?

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
                            (for [entities (partition-all number-columns entities)] ; TODO can just do 1 for?
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

;;

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

; TODO sort them in specific way, id, image first, etc.
; TODO here interleave separators as cells with colspan?
; but for nested map I can check a boolean no separators ?
; https://github.com/kotcrab/vis-ui/blob/4a1e267e80cc38a9467f9bfa67be66902c78b6ef/ui/src/main/java/com/kotcrab/vis/ui/widget/VisTable.java#L45
(defn- ->attribute-widgets [ctx props]
  (for [[k v] (sort-attributes props)
        :let [widget (->attribute-widget ctx [k v])]]
    (do
     (actor/set-id! widget k)
     [(->label ctx (name k)) widget])))

(defn- attribute-widgets->all-data [parent props]
  (println "GET ALL DATA")
  (into {} (for [[k v] props
                 :let [widget (k parent)]
                 :when widget]
             (do
              [k (or (attribute-widget->data widget [k v])
                     v)]))))

;;

(defn ->property-editor-window [context id]
  (let [props (get-property context id)
        {:keys [title
                ; unused
                ; property-keys
                ]} (get property-types (context.properties/property-type props))
        window (->window context {:title (or title (name id))
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
                                                (println "~SAVE ~")
                                                (println " ... get-data")
                                                ; TODO error modal like map editor?
                                                ; TODO refresh overview creatures lvls,etc. ?
                                                (swap! app/current-context properties/update-and-write-to-file! (get-data))
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
