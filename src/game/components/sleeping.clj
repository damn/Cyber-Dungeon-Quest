; TODO should sleeping be an effect ? then how to do 'on-affect' if its not a component?
(ns game.components.sleeping
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.font :as font]
            [gdl.graphics.color :as color]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [game.tick :refer [tick!]]
            [game.render :as render]
            [game.utils.counter :refer [update-counter! make-counter]]
            [game.db :as db]
            [game.media :as media]
            [game.effect :as effect]
            [game.components.faction :as faction]
            [game.modifier :as modifier]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.cell-grid :as cell-grid]
            [game.maps.potential-field :as potential-field]
            [game.components.string-effect :as string-effect]))

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

(defcomponent :sleeping _ ; TODO destructuring optional ? keep code working as is? if-not list ?
  ; skillmanager does not exist yet
  ; so after create
  ; (it seems 'on-create-entity' is the component contructor)
  (db/after-create! [_ entity]
    (swap! entity modifier/apply-modifiers modifiers))

  (render/above [_ {:keys [body]} [x y]]
    (font/draw-text {:font media/font
                     :text "zzz"
                     :x x
                     :y (+ y (:half-height body))
                     :up? true}))

  (render/info [_ entity* position]
    (when (:mouseover? entity*)
      (shape-drawer/circle position aggro-range color/yellow))))

(defn- get-visible-entities [entity* radius]
  (filter #(in-line-of-sight? entity* @%)
          (cell-grid/circle->touched-entities {:position (:position entity*)
                                               :radius radius})))

(declare create-shout-entity!)

(defn- wake-up! [entity]
  (swap! entity #(-> %
                     (dissoc :sleeping)
                     (modifier/reverse-modifiers modifiers)
                     (string-effect/add "!")))
  (create-shout-entity! (:position @entity)
                        (:faction  @entity)))

(defcomponent :shout _
  (tick! [_ entity delta]
    (when (update-counter! entity delta [:counter])
      (swap! entity assoc :destroyed? true)
      ; TODO why a shout checks for ray-blocked? ... sounds logic .... ?!
      (doseq [entity (->> (get-visible-entities @entity aggro-range)
                          (filter #(and (= (:faction @%) (:faction @entity))
                                        (:sleeping @%))))]
        (wake-up! entity)))))

(defn- create-shout-entity! [position faction]
  (db/create-entity! ; entities ?
   {:position position
    :faction faction
    :shout true
    :counter (make-counter 200)})) ; TODO delete-after-duration & on-destroy -> check-wakeup-entities

; could use potential field nearest enemy entity also because we only need 1 (faster)

; also do not need to check every frame !

(defcomponent :sleeping _
  (tick! [_ entity delta]
    ; was performance problem. - or do not check every frame ! -
    #_(when (seq (filter #(not= (:faction @%) (:faction @entity))
                         (get-visible-entities @entity aggro-range)))
        (wake-up! entity))
    (let [cell* @(cell-grid/get-cell (:position @entity))
          faction (faction/enemy (:faction @entity))]
      (when-let [distance (-> cell*
                              faction
                              :distance)]
        (when (<= distance (* aggro-range 10)) ; potential field store as 10  TODO necessary ?
          (wake-up! entity)))))

  (effect/affected! [_ entity]
    (wake-up! entity)))
