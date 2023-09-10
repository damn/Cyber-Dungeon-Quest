(ns game.effects.spawn
  (:require [game.effect :as effect]
            [game.entities.creature :as creature-entity]))

; TODO

; BLOCKING PLAYER MOVEMENT !


; check not blocked position // line of sight.
; limit max. spawns
; animation/sound
; proper icon (grayscaled ?)
; keep in player movement range priority.

; not try-spawn, but check valid-params & then spawn !

; new UI -> show creature body & then place
; >> but what if it is blocked the area during action-time ?? <<

; Also: to make a complete game takes so many creatures, items, skills, balance, ui changes, testing
; is it even possible ?

(defn- do! [{:keys [source target-position value]}]
  (creature-entity/create! value
                           target-position
                           {:faction (:faction @source)}))

(comment
 ; keys: :faction(:source)/:target-position/:creature-id
 )

(effect/defeffect :spawn
  {:text (fn [{:keys [value]}]
           (str "Spawns a " value)) ; pretty name
   :valid-params? (fn [{:keys [source target-position]}]
                    ; TODO line of sight ? / not blocked ..
                    (and source
                         (:faction @source)
                         target-position))
   :do! do!})
