(ns property-editor.screen
  (:require [clojure.edn :as edn]
            [gdl.lifecycle :as lc]
            [gdl.app :as app]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui :as ui]
            [game.properties :as properties])
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.scenes.scene2d.Stage
           (com.badlogic.gdx.scenes.scene2d.ui WidgetGroup)))

; * validation on load all properties / save property/properties
; => spec => key spec
; * items => skill & item both , duplicate pretty-name & image hmmm ....
; or special case for weapon?
; * also item needs to be in certain slot, each slot only once, etc. also max-items ...?
; * non-toggle image-button at overview => VisImageButton
; * missing widgets for keys / one-to-many not implemented

(defn- stage ^Stage []
  (let [{:keys [context/current-screen] :as context} (app/current-context)]
    (:stage (current-screen context))))

(declare property-editor-window)

(defn- open-property-editor-window [id]
  (let [{:keys [gui-viewport-width gui-viewport-height] :as context} (app/current-context)
        window (property-editor-window id)]
    (.addActor (stage) window)
    (actor/set-center window
                      (/ gui-viewport-width  2)
                      (/ gui-viewport-height 2))))

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

(defn- overview-table ^com.badlogic.gdx.scenes.scene2d.ui.Table [property-type clicked-id-fn]
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
                                    :let [on-clicked #(clicked-id-fn id)
                                          button (if (:image props)
                                                   (ui/image-button (:image props) on-clicked)
                                                   (ui/text-button (name id) on-clicked))
                                          top-widget (or (and extra-infos-widget
                                                              (extra-infos-widget props))
                                                         (ui/label ""))
                                          stack (ui/stack)]]
                                (do (actor/set-touchable top-widget :disabled)
                                    (.add stack button)
                                    (.add stack top-widget)
                                    stack)))))))


(defn- add-one-to-many-rows [^com.kotcrab.vis.ui.widget.VisTable table property-type property-ids]
  (.addSeparator table)
  (let [redo-rows (fn [property-ids]
                    (.clearChildren ^com.badlogic.gdx.scenes.scene2d.ui.Table table)
                    (add-one-to-many-rows table property-type property-ids))]
    (ui/add-rows table (concat
                        (for [prop-id property-ids]
                          [(ui/image (ui/texture-region-drawable (:texture (:image (properties/get prop-id))))
                                     :id prop-id)
                           (ui/text-button " - " #(redo-rows (disj (set property-ids) prop-id)))])
                        [[(ui/text-button " + "
                                          (fn []
                                            (let [{:keys [gui-viewport-width gui-viewport-height]} (app/current-context)
                                                  window (ui/window :title "Choose"
                                                                    :modal? true)
                                                  clicked-id-fn (fn [id]
                                                                  (.remove window)
                                                                  (redo-rows (conj (set property-ids) id)))]
                                              (.add window (overview-table property-type clicked-id-fn))
                                              (.pack window)
                                              ; TODO fn above -> open in center .. ?
                                              (.addActor (stage) window)
                                              (actor/set-center window
                                                                (/ gui-viewport-width  2)
                                                                (/ gui-viewport-height 2)))))]])))
  (when-let [parent (.getParent table)]
    (.pack ^WidgetGroup parent)))

(defmethod property-widget :one-to-many [attribute property-ids]
  (let [table (ui/table)]
    (add-one-to-many-rows table (attribute->property-type attribute) property-ids)
    table))

(defmethod widget-data :one-to-many [_ widget]
  (->> (.getChildren ^com.badlogic.gdx.scenes.scene2d.Group widget)
       (keep actor/id)
       set))

(comment

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
                                  (ui/text-button "Cancel" #(.remove window))]]))
    (.pack window)
    window))


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
                     [(ui/text-button (:title overview) #(set-second-widget (overview-table property-type open-property-editor-window)))])
                   [[(ui/text-button "Back to Main Menu" #(app/change-screen! :screens/main-menu))]])))

(defn- create-stage [{:keys [gui-viewport batch]}]
  (let [stage (stage/create gui-viewport batch)
        table (ui/table :id :main-table
                        :rows [[(left-widget) nil]]
                        :fill-parent? true)]
    (.addActor stage table)
    stage))

(defrecord Screen [^Stage stage]
  lc/Disposable
  (lc/dispose [_]
    (.dispose stage))
  lc/Screen
  (lc/show [_ _ctx]
    (.setInputProcessor Gdx/input stage))
  (lc/hide [_]
    (.setInputProcessor Gdx/input nil))
  (lc/render [_ _context]
    (.draw stage))
  (lc/tick [_ _state delta]
    (.act stage delta)))

(defn screen [context]
  (->Screen (create-stage context)))
