(ns game.components.sleeping
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.font :as font]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [game.utils.counter :as counter]
            [game.db :as db]
            [game.entity :as entity]
            [game.components.faction :as faction]
            [game.modifier :as modifier]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.data :refer (get-current-map-data)]
            [game.maps.cell-grid :as cell-grid]
            [game.maps.potential-field :as potential-field]
            [game.components.string-effect :as string-effect])
  (:import com.badlogic.gdx.graphics.Color))

; TODO wake up through walls => sounds are being generated?
; someone is behind a wall and lots of fighting and magic but no line of sight
; or even if he sees in his awareness radius another entity which is attacking player
; they should be able to communicate/hear events through walls

(def aggro-range 6)

; :movement/:skillmanager, basically not sleeping can patrol but not 'hostile' to player
; -> later just :hostile switch state
(def ^:private modifiers
  [[:modifiers/block :speed]
   [:modifiers/block :skillmanager]])

(defcomponent :sleeping _
  (entity/create! [_ entity _ctx]
    (swap! entity modifier/apply-modifiers modifiers))
  (entity/render-above [_ context {[x y] :position :keys [body]}]
    (font/draw-text context
                    {:text "zzz"
                     :x x
                     :y (+ y (:half-height body))
                     :up? true}))
  (entity/render-info [_ context {:keys [position mouseover?]}]
    (when mouseover?
      (shape-drawer/circle position aggro-range Color/YELLOW))))

(defn- get-visible-entities [cell-grid entity* radius context]
  (filter #(in-line-of-sight? entity* @% context)
          (cell-grid/circle->touched-entities cell-grid
                                              {:position (:position entity*)
                                               :radius radius})))

(defn- create-shout-entity [position faction]
  {:position position
   :faction faction
   :shout (counter/create 200)})

(defn- wake-up! [entity context]
  (swap! entity #(-> %
                     (dissoc :sleeping)
                     (modifier/reverse-modifiers modifiers)
                     (string-effect/add "!")))
  (db/create-entity! (create-shout-entity (:position @entity)
                                          (:faction  @entity))
                     context))

(defcomponent :shout counter
  (entity/tick [_ delta]
    (counter/tick counter delta))
  (entity/tick! [_ context entity delta]
    (when (counter/stopped? counter)
      (swap! entity assoc :destroyed? true)
      ; TODO why a shout checks for ray-blocked? ... sounds logic .... ?!
      (doseq [entity (->> (get-visible-entities (:cell-grid (get-current-map-data))
                                                @entity
                                                aggro-range
                                                context)
                          (filter #(and (= (:faction @%) (:faction @entity))
                                        (:sleeping @%))))]
        (wake-up! entity context)))))

; could use potential field nearest enemy entity also because we only need 1 (faster)
; also do not need to check every frame !
(defcomponent :sleeping _
  (entity/tick! [_ context entity delta]
    ; was performance problem. - or do not check every frame ! -
    #_(when (seq (filter #(not= (:faction @%) (:faction @entity))
                         (get-visible-entities @entity aggro-range)))
        (wake-up! entity))
    (let [cell-grid (:cell-grid (get-current-map-data))
          cell* @(get cell-grid (mapv int (:position @entity)))
          faction (faction/enemy (:faction @entity))]
      (when-let [distance (-> cell*
                              faction
                              :distance)]
        (when (<= distance (* aggro-range 10)) ; potential field store as 10  TODO necessary ?
          (wake-up! entity context)))))
  (entity/affected! [_ entity context]
    (wake-up! entity context)))
