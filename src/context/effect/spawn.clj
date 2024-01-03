(ns context.effect.spawn
  (:require [context.effect :as effect]
            [cdq.context :refer [creature-entity]]))

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

(defmethod effect/do! :effect/spawn
  [{:keys [effect/source
           effect/target-position] :as context}
   [_ creature-id]]
  (creature-entity context
                   creature-id
                   target-position
                   ; TODO make this param explicit @ create-creature-data, don't just merge there.
                   {:entity/faction (:entity/faction @source)
                    :initial-state :idle}))
