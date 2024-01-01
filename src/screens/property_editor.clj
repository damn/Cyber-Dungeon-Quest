(ns screens.property-editor
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [gdl.app :as app :refer [change-screen!]]
            [gdl.context :refer [get-stage ->text-button ->image-button ->label ->text-field ->image-widget ->table ->stack ->window all-sound-files play-sound! ->vertical-group]]
            [gdl.scene2d.actor :as actor :refer [remove! set-touchable! parent add-listener! add-tooltip!]]
            [gdl.scene2d.group :refer [add-actor! clear-children! children]]
            [gdl.scene2d.ui.text-field :as text-field]
            [gdl.scene2d.ui.table :refer [add! add-rows cells]]
            [gdl.scene2d.ui.cell :refer [set-actor!]]
            [gdl.scene2d.ui.widget-group :refer [pack!]]
            context.modifier
            context.effect
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
                                                (default-property-tooltip-text ctx props))))}}
   :misc {:title "Misc"
          :overview {:title "Misc"
                     :tooltip-text-fn default-property-tooltip-text}}})
;;

(defn- one-to-many-attribute->linked-property-type [k]
  (case k
    :skills :skill
    :items :item))

(def ^:private attribute->value-widget
  {:id :label
   :image :image
   :pretty-name :text-field

   :slot :label
   :two-handed? :label

   :level :text-field
   :species :link-button
   :skills :one-to-many
   :items :one-to-many

   :modifier :nested-map
   :modifier/armor :text-field
   :modifier/max-mana :text-field

   :spell? :label
   :action-time :text-field
   :cooldown :text-field
   :cost :text-field
   :effect :nested-map

   :effect/sound :sound
   :effect/damage :text-field

   :effect/target-entity :nested-map
   :maxrange :text-field
   :hit-effect :nested-map

   :map-size :text-field
   :max-area-level :text-field
   :spawn-rate :text-field
   })

(defn- removable? [k]
  (#{"effect" "modifier"} (namespace k)))

(defn- sort-attributes [properties]
  (sort-by
   (fn [[k _v]]
     [(case k
        :id 0
        :image 1
        :pretty-name 2
        :spell? 3
        :level 3
        :slot 3
        :two-handed? 4
        :species 4
        9)
      (name k)])
   properties))

;;

(defmulti ->value-widget     (fn [_ctx [k _v]] (get attribute->value-widget k)))
(defmulti value-widget->data (fn [k _widget]   (get attribute->value-widget k)))

;;

(defn ->edn [v]
  (binding [*print-level* nil]
    (pr-str v)))

(defmethod ->value-widget :default [ctx [_ v]]
  (->label ctx (->edn v)))

(defmethod value-widget->data :default [_ widget]
  (actor/id widget))

;;

(defmethod ->value-widget :text-field [ctx [_ v]]
  (->text-field ctx (->edn v) {}))

(defmethod value-widget->data :text-field [_ widget]
  (edn/read-string (text-field/text widget)))

;;

(defn ->scroll-pane [_ actor]
  (let [widget (com.kotcrab.vis.ui.widget.VisScrollPane. actor)]
    (.setFlickScroll widget false)
    (.setFadeScrollBars widget false)
    ; TODO set touch focus , have to click first to use scroll pad
    ; TODO use scrollpad ingame too
    widget))

(defn ->scrollable-choose-window [ctx rows]
  (let [window (->window ctx {:title "Choose"
                              :modal? true
                              :close-button? true
                              :center? true
                              :close-on-escape? true})
        table (->table ctx {:rows rows
                            :cell-defaults {:pad 1}})]

    ; (println "(.getWidth table)" (.getWidth table))
    ; == 0 .. ?1
    ; or resizable make window ... ?
    ; or pass size at params

    (.width
     (.height (.add window (->scroll-pane ctx table))
              (float (- (:gui-viewport-height ctx) 50)))
     (float (+ 100 (/ (:gui-viewport-width ctx) 2)))
     )
    (.pack window)
    window))

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

(defmethod ->value-widget :image [ctx [_ image]]
  (->image-button ctx image #(add-to-stage! % (->scrollable-choose-window % (texture-rows %)))))

;;

(declare ->property-editor-window)

(defn open-property-editor-window! [context property-id]
  (add-to-stage! context (->property-editor-window context property-id)))

(defmethod ->value-widget :link-button [context [_ prop-id]]
  (->text-button context (name prop-id) #(open-property-editor-window! % prop-id)))

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
    :effect     (keys (methods context.effect/do!))
    :hit-effect (keys (methods context.effect/do!)) ; only those with 'source/target'
    ))

(declare ->attribute-widget-group)

#_(defn- clicked-nested-map-overview [k ctx table window nested-k widget-rows]
  (remove! window)
  (redo-nested-map-rows! k ctx table
                         (conj widget-rows (first (->attribute-widget-group ctx {nested-k nil})))))

#_(defn- ->nested-map-rows [k ctx table widget-group]
  (conj (for [row (children widget-group)]
          (conj row (->text-button ctx "-" #(redo-nested-map-rows! k % table (disj widget-rows row)))))
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

#_(defn- redo-nested-map-rows! [k ctx table widget-rows]
  (clear-children! table)
  (add-rows table (->nested-map-rows k ctx table widget-rows))
  ; TODO recursively pack all parents ! hit-effect ..
  (pack! (parent table)))

; FIXME target-entity does not have 'add'/'remove' nested fields ....
(defmethod ->value-widget :nested-map [ctx [_ props]]
  (->attribute-widget-group ctx props))

(declare attribute-widget-group->data)

(defmethod value-widget->data :nested-map [_ group]
  (attribute-widget-group->data group))

; FIXME
;Assert failed: Actor ids are not distinct: [:effect/damage :effect/damage :effect/stun]
;(or (empty? ids) (apply distinct? ids))
; => don't always save as map ?

(comment
 ; use vertical group ?
 ; each element = one attribute [k v] and separator table ?
 ; can remove/add easily ?
 ; cannot remove rows, ahve to redo ..
 (let [ctx @gdl.app/current-context
       window (gdl.context/mouse-on-stage-actor? ctx)
       number-rows (.getRows window)
       number-columns (.getColumns window)
       cells (seq (.getCells window))
       ; rows (partition number-columns cells)
       ]
   (clojure.pprint/pprint (map (fn [cell] [(.getRow cell) cell]) cells))
   ; some rows only separator
   ; how do I know when is a new row???
   )
 )

;;

; TODO select from all sounds (see duration, waveform, if already used somewhere ?)
; TODO selectable sound / pass widget->data somehow
; TODO do the same for image!!
; TODO -> modal window reuse code?

; FIXME sound saving becomes nil because nested bla uses widget-data fns not 'or'
; => probably should always fetch the data out of the widget not original props
; even for label ... or image ... or sound ...
; => store the value somewhere ? id [k v] ?
; but k parent used a lot dont even know where ...
; or hidden value id widget o.o

; TODO why not effect text 'spawns a wizard' ?

(defn- ->play-sound-button [ctx sound-file]
  (->text-button ctx ">>>" #(play-sound! % sound-file)))

(defn- all-sounds-rows [ctx]
  (for [sound-file (all-sound-files ctx)]
    [(->text-button ctx (str/replace-first sound-file "sounds/" "")
                    (fn [_ctx]
                      (println "selected " sound-file)
                      ; TODO add sound widget, clear table cells? idk lets see...
                      ))
     (->play-sound-button ctx sound-file)]))

(defn- click-open-sounds-window! [ctx]
  (add-to-stage! ctx (->scrollable-choose-window ctx (all-sounds-rows ctx))))

(defn- ->sound-button [ctx sound-file]
  (let [button (->text-button ctx (name sound-file) click-open-sounds-window!)]
    (actor/set-id! button sound-file)
    button))

(defmethod ->value-widget :sound [ctx [_ sound-file]]
  (->table ctx {:cell-defaults {:pad 5}
                :rows [(if sound-file
                         [(->sound-button ctx sound-file)
                          (->play-sound-button ctx sound-file)]
                         [(->text-button ctx "Select sound" click-open-sounds-window!)])]}))

(defmethod value-widget->data :sound [_ widget]
  (actor/id (first (children widget))))

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
                                   (add-tooltip! button #(tooltip-text-fn % props)))
                                 (set-touchable! top-widget :disabled)
                                 stack))))})))

;;

(defn- add-one-to-many-rows [ctx table property-type property-ids]
  (let [redo-rows (fn [ctx property-ids]
                    (clear-children! table)
                    (add-one-to-many-rows ctx table property-type property-ids))]
    (add-rows table (concat
                     (for [prop-id property-ids]
                       [(let [props (get-property ctx prop-id)
                              image-widget (->image-widget ctx ; TODO image-button (link)
                                                           (:image props)
                                                           {:id (:id props)})]
                          (add-tooltip! image-widget #((-> property-types
                                                               property-type
                                                               :overview
                                                               :tooltip-text-fn) % props))
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

(defmethod ->value-widget :one-to-many [context [attribute property-ids]]
  (let [table (->table context {})]
    (add-one-to-many-rows context
                          table
                          (one-to-many-attribute->linked-property-type attribute)
                          property-ids)
    table))

(defmethod value-widget->data :one-to-many [_ widget]
  (->> (children widget) (keep actor/id) set))

;;

; TODO separators don't work with nested yet, also probably fucking up children actor id
; also they are removable o.o

(defn- ->horizontal-separator-cell [colspan]
  {:actor (com.kotcrab.vis.ui.widget.Separator. "default")
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn- ->vertical-separator-cell []
  {:actor (com.kotcrab.vis.ui.widget.Separator. "vertical")
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(defn ->attribute-widget-rows [ctx [k v] & {:keys [horizontal-sep? table]}]
  (let [label (->label ctx (name k))
        value-widget (->value-widget ctx [k v])
        column (remove nil?
                       [(when (removable? k)
                          (->text-button ctx "-" (fn [_ctx] (remove! table))))
                        label
                        (->vertical-separator-cell)
                        value-widget])
        rows [(when horizontal-sep? [(->horizontal-separator-cell (count column))])
              column]]
    (actor/set-id! value-widget v)
    (remove nil? rows)))

(defn- attribute-widget-table->value-widget [table]
  (-> table children last))

(defn- ->attribute-widget-tables [ctx props]
  (let [first-row? (atom true)]
    (for [[k v] (sort-attributes props)
          :let [sep? (not @first-row?)
                _ (reset! first-row? false)
                table (->table ctx {:id k
                                    :cell-defaults {:pad 4}})]]
      (do
       (add-rows table (->attribute-widget-rows ctx [k v]
                                                :horizontal-sep? sep?
                                                :table table))
       table))))

(defn- ->attribute-widget-group [ctx props]
  (let [group (->vertical-group ctx (->attribute-widget-tables ctx props))]
    ;(.left group)
    group
    ))

(defn- attribute-widget-group->data [group]
  (for [k (map actor/id (children group))
        :let [table (k group)
              value-widget (attribute-widget-table->value-widget table)]]
    [k (value-widget->data k value-widget)]))

; value-widget->data can be removed mostly only for text-field not
; because I will just set the value id as of changes ... ?

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
        widgets (->attribute-widget-group context props)]
    (add-rows window [[widgets]
                      [(->text-button context "Save"
                                      (fn [_ctx]
                                        ; TODO error modal like map editor?
                                        ; TODO refresh overview creatures lvls,etc. ?
                                        (swap! app/current-context properties/update-and-write-to-file!
                                               (into {} (attribute-widget-group->data widgets)))
                                        (remove! window)))]])
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
                           (for [[property-type {:keys [overview]}] (select-keys property-types [:creature :item :skill :weapon :misc])]
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
