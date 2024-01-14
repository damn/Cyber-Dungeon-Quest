(ns cdq.context.game
  (:require [cdq.context.counter :as counter]
            [cdq.context.ecs :as ecs]
            [cdq.context.mouseover-entity :as mouseover-entity]
            [cdq.context.ui.player-message :as player-message]
            [cdq.context.transaction-handler :as txs]
            [cdq.context.world :as world]
            [cdq.context :refer [rebuild-inventory-widgets reset-actionbar]]))

(defn- fetch-player-entity [ctx]
  {:post [%]}
  (first (filter #(:entity/player? @%)
                 (vals @(:cdq.context.ecs/uids->entities ctx))))) ; TODO private ! move to ecs ! forgot uid change

(defn start-game-context [ctx]
  (rebuild-inventory-widgets ctx)
  (reset-actionbar ctx)
  (let [ctx (merge ctx
                   (ecs/->context :z-orders [:z-order/on-ground
                                             :z-order/ground
                                             :z-order/flying
                                             :z-order/effect])
                   (mouseover-entity/->context)
                   (player-message/->context)
                   (counter/->context)
                   {:context/game-paused? (atom true)
                    :context/game-logic-frame (atom 0)}
                   (world/->context ctx))]

    (reset! txs/frame->txs {})
    (.bindRoot #'txs/record-txs? true)
    (.bindRoot #'cdq.screens.game/replay-game? false)
    (println "Starting world - txs/record-txs? " txs/record-txs?)
    (println "~~ logging initial txs - " [(keys @txs/frame->txs) (map count (vals @txs/frame->txs))])
    (println "transact-create-entities-from-tiledmap!")
    (world/transact-create-entities-from-tiledmap! ctx)
    (println "~~ logging initial txs - " [(keys @txs/frame->txs) (map count (vals @txs/frame->txs))])
    (println "Initial entity txs:")
    (clojure.pprint/pprint
     (for [[txk txs] (group-by first (second (first @txs/frame->txs)))]
       [txk (count txs)]))

    (assoc ctx :context/player-entity (fetch-player-entity ctx))))

(require '[cdq.context.transaction-handler :as txs])

(comment

  ; TODO tx/destroy 'plop' triggers
  ; appearing !
  ; I want to clean up the whole ecs system
  ; => recreate ecs/world context
  ; (Only I don't have rand-gen for map & spawn entities )
  ; that means world-grid,content-grid, ? explored-tiles? (TODO)

  ; #1 #1 !
  ; -> clear up ecs / world(?) completely

  ; => use change-screen ? and on enter/exit I can change stuffs
  ; (cannot share actors game/replay screen !

  (.postRunnable com.badlogic.gdx.Gdx/app
                 (fn []
                   (let [ctx @gdl.app/current-context
                         entities (vals @(:cdq.context.ecs/uids->entities ctx))
                         initial-txs (cdq.context/frame->txs ctx 0)]
                     (println "(count initial-txs)" (count initial-txs))
                     (println "Initial-txs:")
                     (clojure.pprint/pprint
                      (for [[txk txs] (group-by first initial-txs)]
                        [txk (count txs)]))

                     ; remove all entities
                     ; cleanup / >dispose< / on-destroy ? = plop
                     ; call fn dispose all entities (because I keep world, and remove references there)
                     ; then recreate ecs
                     (cdq.context/transact-all! ctx (for [e entities] [:tx/destroy e]))
                     (cdq.context/remove-destroyed-entities! ctx)

                     ; reset UI
                     (cdq.context/rebuild-inventory-widgets ctx)
                     (cdq.context/reset-actionbar ctx)

                     ; reset counters
                     (reset! (:context/game-logic-frame ctx) 0) ; <- part of replay-game-screen enter

                     ; Do not log the replayed txs !
                     (.bindRoot #'txs/record-txs? false) ; <- part of replay-game-screen enter
                     ; set game to replay loop
                     (.bindRoot #'cdq.screens.game/replay-game? true)

                     ; those 2 below also part of replay-game-screen enter
                     ; apply initial txs
                     (cdq.context/transact-all! ctx initial-txs)

                     ; set up player-entity
                     (swap! gdl.app/current-context merge {:context/player-entity (fetch-player-entity ctx)})

                     (println "frames/txs - " [(keys @txs/frame->txs) (map count (vals @txs/frame->txs))])))))


