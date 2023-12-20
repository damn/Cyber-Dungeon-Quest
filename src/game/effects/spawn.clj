(ns game.effects.spawn
  (:require [game.effect :as effect]
            [game.context :refer [creature-entity]]))

; BLOCKING PLAYER MOVEMENT !
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

(effect/defeffect :spawn
  {:text (fn [creature-id _params _context]
           (str "Spawns a " creature-id)) ; pretty name
   :valid-params? (fn [_effect-val {:keys [source target-position]} _context]
                    ; TODO line of sight ? / not blocked ..
                    (and source
                         (:faction @source)
                         target-position))
   :do! (fn [creature-id {:keys [source target-position]} context]
          (creature-entity context
                           creature-id
                           target-position
                           {:faction (:faction @source)
                            :initial-state :idle}))})
