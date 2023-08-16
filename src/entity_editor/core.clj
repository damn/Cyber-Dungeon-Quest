(ns entity-editor.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [seesaw.core :as seesaw]
            game.effects.core
            game.components.modifiers)
  (:import [javax.imageio ImageIO]
           [javax.swing JTable ImageIcon]
           [javax.swing.table DefaultTableModel]))

; TODO set editable? for columns
; and class
; TODO make some columns button (links) -> creature-type to that type ?
; TODO table.getModel().addTableModelListener(this); for changes
; -> autosave them
; TODO column width as max-width of the longest element
; multiple -> list , weapons combobox? searchable ?

(def resources "resources/")

(def table-properties
  [{:rows (map (fn [k] {:id k}) (keys game.effects.core/effect-definitions))
    :title "Effects"
    :columns [:id]}

   {:rows (map (fn [k] {:id k}) (keys game.components.modifiers/modifier-definitions))
    :title "Modifiers"
    :columns [:id]}

   {:file "creatures/creatures.edn"
    :title "Creatures"
    :columns [:id :image :creature-type :level :items :skills]}

   {:file "creatures/creature-types.edn"
    :title "Creature Types"
    :columns [:id :image :hp :speed]}

   {:file "skills/properties.edn"
    :title "Spells"
    :columns [:id :image :action-time :cooldown :cost :effect]}

   {:file "items/properties.edn"
    :title "Items"
    :columns [:id :image :slot :two-handed]}

   {:file "items/properties.edn"
    :title "Weapons"
    :columns [:id :image :action-time :damage :range]} ; TODO only display slot=weapon

   {:file "properties.edn"
    :title "Misc Properties"
    :columns [:id :action-time :damage :range]}])

(def read-image ; read sprite-sheets only once.
  (memoize
   (fn [file]
     (ImageIO/read (io/file (str resources file))))))

(defn image->icon [{:keys [file sub-image-bounds]}]
  (try
   (let [image (read-image file)
         [x y w h] sub-image-bounds]
     (seesaw.icon/icon (.getSubimage image x y w h)))
   (catch Exception e
     file)))

(comment
 (image->icon
  {:file "creatures/images.png",
   :sub-image-bounds [144 0 48 48]}))

(defn- load-edn [file]
  (->> file
       (str resources)
       slurp
       edn/read-string
       (map #(if (:image %)
               (update % :image image->icon)
               %))))

(comment
 (use 'seesaw.dev)

 (show-events (seesaw/table))
 ; :property-change

 (show-events (seesaw.table/table-model))

 (clojure.pprint/pprint
  (bean event-tmp))

 )

(defn- make-table [{:keys [title columns rows]}]
  (println "Table: " title " , columns: " columns, " rows: " (count rows))
  (let [table (seesaw/table
               :background (seesaw.color/color 200 200 200)
               :id title ; TODO title == id !
               :model [:columns (map (fn [column]
                                        {:key column
                                         :class (if (= column :image)
                                                  ImageIcon
                                                  Object)})
                                      columns)
                        :rows rows])]
    (seesaw/listen table :property-change (fn [e]
                                            (when (= "tableCellEditor" (.getPropertyName e))
                                              (println "Edited: " (def event-tmp e)))))
    (.setAutoCreateRowSorter table true)
    (.setRowHeight table 48)
    table))

(defn- make-tabs []
  (let [table-properties (map #(if (:file %)
                                 (assoc % :rows (load-edn (:file %)))
                                 %)
                              table-properties)]
     (map (fn [{:keys [title] :as properties}]
          {:title title
           :content (seesaw/scrollable
                     (make-table properties))})
        table-properties)))

(defn create-property-editor []
 (seesaw/native!)
 (def f (seesaw/frame :title "Property Editor"))
 (-> f
     seesaw/pack!
     seesaw/show!)
 (seesaw/config! f :content (seesaw/tabbed-panel
                             :placement :top
                             :tabs (make-tabs)))
 (seesaw/pack! f))

(comment
 (create-property-editor)
 )

(comment
 (require 'vlaaad.reveal)
 )



(comment


 (import 'javax.swing.JComboBox)
 (import 'javax.swing.DefaultCellEditor)

 (let [items-column  (.getColumn (.getColumnModel (seesaw/select f [:#Creatures]))
                                 4)
       combo-box (JComboBox.)]
   (doseq [{:keys [id]} (load-edn "items/properties.edn")]
     (.addItem combo-box (name id)))

   (.setCellEditor items-column
                   (DefaultCellEditor. combo-box))

   )

 (clojure.pprint/pprint
  (first
   (filter #(= (:id %) :archer)
           (let [table (seesaw/select f [:#Creatures])
                 row-count (seesaw.table/row-count table)
                 ]
             row-count
             (seesaw.table/value-at
              table
              (range row-count)
              )
             ))))



 )

; https://github.com/clj-commons/seesaw/blob/master/src/seesaw/table.clj#L46C39-L46C39
; isCellEditable false !
; ask in groups!


; TODO image -> URL image label
; sub image

; -> make java.awt.Component yourself @ imaeg !


; create TABLE


; TODO for properties.edn
; -> create a plain table viewer for each property namespace
; with max-keys and tabs
; so any new properties visible/editable.
; also possible to add categories/properties/remove/etc.








; * tree
; * table
; * how tf to render a table with images (sub-images) of sprites ???



; TODO
; * render image / sub-image ( icon )

; BufferedImage
; getSubimage(int x, int y, int w, int h)
; Returns a subimage defined by a specified rectangular region.
