; TODO move to x.session
(nsx game.player.session-data
  (:require [clojure.walk :refer [postwalk]]
            game.maps.cell-grid
            game.maps.data
            game.maps.load
            (game.utils msg-to-player)
            game.items.inventory
            game.screens.options
            game.ui.action-bar)
  (:use (game.maps add impl)))


(def current-character-name (atom nil))

;; TODO read/write/load/get -> move to x.session ?
;; different character name -> separate file ?

(def ^:private file (str "cdq_savegame.clj")) ; .edn ?

(defn- read-session-file []
  (try (read-string (slurp file))
    (catch java.io.FileNotFoundException e {})))

(defn- pr-str-with-meta [x]
  (binding [*print-meta* true]
    (pr-str x)))

(defn- write-session-file [data]
  (->> (postwalk session/write-to-disk data)
       (assoc (read-session-file) @current-character-name)
       pr-str-with-meta
       (spit file)))

(defn get-session-file-character-names []
  (keys (read-session-file)))

(defn get-session-file-data []
  (->> (get (read-session-file) @current-character-name)
       (postwalk session/load-from-disk)))

;;

(defn- make-type [v] ; ??? => var-namespaced-name-str
  (.replace (str (ns-name (:ns (meta v))) "/" (:name (meta v))) "game." ""))

(def state (reify session/State
             (load! [_ _]
               (add-maps-data (first-level)))
             (serialize [_])
             (initial-data [_])))


; TODO dont need vars here, because this is a function? why is it a function anyway?
(defn- session-components []
  (map #(vector @% (make-type %))
       [; resets all map data -> do it before creating maps => TODO put both in one maps-session-component?
        #'game.maps.data/state

        ; create maps before putting entities in them
        #'game.player.session-data/state

        ; resets all entity data -> do it before addding entities
        #'game.db/db-state

        ; reset cell-change-listeners before adding entities, because entities are listeners
        ; => TODO do @ map
        #'game.maps.cell-grid/state

        #'game.items.inventory/state
        ; :inventory-window

        ; adding entities (including player-entity)
        #'game.maps.load/state

        ; now the order of initialisation does not matter anymore
        #'game.ui.action-bar/state
        #'game.screens.options/state
        #'game.ui.mouseover-entity/state
        #'game.utils.msg-to-player/state]))

(assert (distinct-seq? (session-components)))
; The types need to be distinct, so load-session-component can get the data from the savegame-file that corresponds to the right session-component

(defn save-game [] ; TODO pass argument file & states, move to x.session
  (->> (for [[component stype] (session-components)]
         (when-let [savedata (session/serialize component)]
           [stype savedata]))
       (into {})
       write-session-file))

; TODO rename 'load-game' ?
(defn init [is-loaded-character] ; TODO move to x.session
  (let [session-file-data (when is-loaded-character
                            (get-session-file-data))]
    (doseq [[component stype] (session-components)]
      ;(println "Load state: " stype)
      (session/load! component
                   (if is-loaded-character
                     (get session-file-data stype)
                     (session/initial-data component))))))
