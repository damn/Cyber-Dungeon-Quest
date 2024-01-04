(ns cdq.effect.spawn
  (:require [malli.core :as m]
            [cdq.effect :as effect]
            [cdq.entity.state.npc :as npc-state]
            [cdq.context :refer [creature]]))

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

(def ^:private schema
  (m/schema [:qualified-keyword {:namespace :creatures}]))

(defmethod effect/value-schema :effect/spawn [_]
  schema)

(defmethod effect/text :effect/spawn
  [_context [_ creature-id]]
  (str "Spawns a " (name creature-id)))

(defmethod effect/valid-params? :effect/spawn
  [{:keys [effect/source
           effect/target-position]} _effect]
  ; TODO line of sight ? / not blocked ..
  (and source
       (:entity/faction @source)
       target-position))

(defmethod effect/transactions :effect/spawn
  [{:keys [effect/source
           effect/target-position] :as context}
   [_ creature-id]]
  [(creature context
             creature-id
             target-position
             {:entity/state (npc-state/->state :idle)
              :entity/faction (:entity/faction @source)})])
