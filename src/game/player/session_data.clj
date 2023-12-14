(ns game.player.session-data
  (:require [x.x :refer [update-map doseq-entity]]
            game.context
            [game.entity :as entity]
            [gdl.app :as app]
            [game.session :as session]
            game.maps.add
            game.maps.impl
            game.maps.data
            game.maps.load
            game.utils.msg-to-player
            game.screens.options
            game.ui.action-bar
            game.ui.inventory-window))

(extend-type gdl.app.Context
  game.context/Context
  (get-entity [{:keys [context/ids->entities]} id]
    (get @ids->entities id))

  (entity-exists? [context e]
    (game.context/get-entity context (:id @e)))

  (create-entity! [context components-map]
    {:pre [(not (contains? components-map :id))]}
    (-> (assoc components-map :id nil)
        (update-map entity/create)
        atom
        (doseq-entity entity/create! context)))

  (destroy-to-be-removed-entities!
    [{:keys [context/ids->entities] :as context}]
    (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))
            :when (game.context/entity-exists? context e)] ; TODO why is this ?
      (swap! e update-map entity/destroy)
      (doseq-entity e entity/destroy! context))))

(def state (reify session/State
             (load! [_ _]
               (game.maps.add/add-maps-data
                (game.maps.impl/first-level)))
             (serialize [_])
             (initial-data [_])))

(def ^:private session-components
  [; resets all map data -> do it before creating maps
   game.maps.data/state
   ; create maps before putting entities in them
   game.player.session-data/state
   game.ui.inventory-window/state
   ; adding entities (including player-entity)
   game.maps.load/state
   ; now the order of initialisation does not matter anymore
   game.ui.action-bar/state
   game.screens.options/state
   game.ui.mouseover-entity/state
   game.utils.msg-to-player/state])

(defn init []
  (swap! app/state assoc :context/ids->entities (atom {}))
  (doseq [component session-components]
    (session/load! component
                   (session/initial-data component))))
