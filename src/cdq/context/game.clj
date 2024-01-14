(ns cdq.context.game
  (:require [cdq.context.counter :as counter]
            [cdq.context.ecs :as ecs]
            [cdq.context.mouseover-entity :as mouseover-entity]
            [cdq.context.ui.player-message :as player-message]
            [cdq.context.transaction-handler :as txs]
            [cdq.context.world :as world]
            [cdq.context :refer [rebuild-inventory-widgets reset-actionbar frame->txs transact-all!]]))

; TODO private ! move to ecs ! forgot uid change
; (use custom uid ?)
(defn- fetch-player-entity [ctx]
  {:post [%]}
  (first (filter #(:entity/player? @%)
                 (vals @(:cdq.context.ecs/uids->entities ctx)))))

(defn start-new-game [ctx]
  (rebuild-inventory-widgets ctx)
  (reset-actionbar ctx)
  (let [ctx (merge ctx
                   (ecs/->context :z-orders [:z-order/on-ground
                                             :z-order/ground
                                             :z-order/flying
                                             :z-order/effect])
                   (mouseover-entity/->context)
                   (player-message/->context)
                   (counter/->context) ; = elapsed game time counter
                   {:context/game-paused? (atom nil)
                    :context/game-logic-frame (atom 0)
                    :context/replay-mode? false}
                   (world/->context ctx))]
    (txs/clear-recorded-txs!)
    (txs/set-record-txs! true)
    (world/transact-create-entities-from-tiledmap! ctx)
    (println "Initial entity txs:")
    (txs/summarize-txs (frame->txs ctx 0))
    (assoc ctx :context/player-entity (fetch-player-entity ctx))))

; TODO tx/destroy 'plop' triggers
; appearing ! ->

; completely clear up ecs (after disposing world connections ! -> call entity/dispose ?)

; explored-tiles? (TODO)
; player message, player modals, etc. all game related state handle ....
; game timer is not reset  - continues as if
; entities all disappearing, just stop when end reached ....
; check other atoms , try to remove atoms ...... !?

(defn- start-replay-mode! [ctx]

  ; clear up game context ( keep world !)
  (cdq.context/transact-all! ctx (for [e (vals @(:cdq.context.ecs/uids->entities ctx))] [:tx/destroy e]))
  (cdq.context/remove-destroyed-entities! ctx)
  (cdq.context/rebuild-inventory-widgets ctx)
  (cdq.context/reset-actionbar ctx)
  (reset! (:context/game-logic-frame ctx) 0)

  ; place initial entities
  (txs/set-record-txs! false)
  (let [initial-txs (frame->txs ctx 0)]
    (transact-all! ctx initial-txs))
  (swap! gdl.app/current-context merge
         {:context/replay-mode? true
          :context/player-entity (fetch-player-entity ctx)}))

(comment

  (.postRunnable com.badlogic.gdx.Gdx/app (fn [] (start-replay-mode! @gdl.app/current-context) )))


