(ns cdq.effect.spawn
  (:require [x.x :refer [defcomponent]]
            [cdq.context :refer [transact!]]
            [cdq.effect :as effect]
            [cdq.state.npc :as npc-state]))

; TODO spawning on player both without error ?! => not valid position checked
; also what if someone moves on the target posi ? find nearby valid cell ?

; BLOCKING PLAYER MOVEMENT ! (summons no-clip with player ?)
; check not blocked position // line of sight.
; limit max. spawns
; animation/sound
; proper icon (grayscaled ?)
; keep in player movement range priority ( follow player if too far, otherwise going for enemies)
; => so they follow you around

; not try-spawn, but check valid-params & then spawn !

; new UI -> show creature body & then place
; >> but what if it is blocked the area during action-time ?? <<

; Also: to make a complete game takes so many creatures, items, skills, balance, ui changes, testing
; is it even possible ?

(comment
 ; keys: :faction(:source)/:target-position/:creature-id
 )

(defcomponent :tx/spawn creature-id
  (effect/text [_ _ctx]
    (str "Spawns a " (name creature-id)))

  (effect/valid-params? [_ {:keys [effect/source
                                   effect/target-position]}]
    ; TODO line of sight ? / not blocked ..
    (and source
         (:entity/faction @source)
         target-position))

  (transact! [_ {:keys [effect/source
                        effect/target-position] :as ctx}]
    [[:tx/creature
      creature-id
      target-position
      {:entity/state (npc-state/->state :idle)
       :entity/faction (:entity/faction @source)}]]))
