(ns cdq.context.game
  (:require [cdq.context.counter :as counter]
            [cdq.context.ecs :as ecs]
            [cdq.context.mouseover-entity :as mouseover-entity]
            [cdq.context.ui.player-message :as player-message]
            [cdq.context.world :as world]
            [cdq.context :refer [rebuild-inventory-widgets reset-actionbar]]))

; include in new-game-context:
; cdq.app.start - game screen actors clear, inventory-window, actionbar
; make interface
; (w/o world ?)
; cursor also?

; TODO recored explored-tile-corners ??!
; or recalculate on the fly with render-map ? just reset ?

; => stateful context somehow mark/remember/list

; can it be simpler?o
; ecs/everything already set up with render orders
; just fn / system -> start-new-game-session

; see in the context tree
; 'game-specific' context components
; which get reset then

; all these flags/components/resets
; make with a system.

(defn start-game-context [context]

  ; just move into common fn
  (rebuild-inventory-widgets context)
  (reset-actionbar context)

  (let [context (merge context
                       (ecs/->context :z-orders [:z-order/on-ground
                                                 :z-order/ground
                                                 :z-order/flying
                                                 :z-order/effect])
                       (mouseover-entity/->context)
                       (player-message/->context)
                       (counter/->context)
                       {:context/game-paused? (atom true)
                        :context/game-logic-frame (atom 0)
                        })]
    (world/merge->context context)))
