(ns game.effects.restore-hp-mana
  (:require [gdl.audio :as audio]
            [data.val-max :refer [lower-than-max? remainder-to-max]]
            [game.effect :as effect]
            [game.components.skills :refer (ai-should-use?)]
            game.effects.hp
            game.effects.mana))

(defmethod ai-should-use? :restore-hp-mana [_ entity]
  (or (lower-than-max? (:mana @entity))
      (lower-than-max? (:hp   @entity))))

; TODO make with 'target' then can use as hit-effect too !
(effect/defeffect :restore-hp-mana
  {:text (fn [_] "Restores full hp and mana.")
   :valid-params? (fn [{:keys [source]}]
                    source)
   :do! (fn [{:keys [source]}]
          (audio/play "bfxr_drugsuse.wav")
          (effect/do-all!
           {:target source}
           [[:hp   [[:val :inc] (remainder-to-max (:hp   @source))]]
            [:mana [[:val :inc] (remainder-to-max (:mana @source))]]]))})


; TODO create components with symbols as name
; when they have to refer to a special thing??
; compile time checking of symbol-components ?
; :hit-effects [effect/damage 3] ; ???
; or on-create will get checked ???
; data based !
#_(defcomponent effect/restoration
    (ai-should-use? [entity*]
      ; ...
      )
    (text [_ v]
      "Restores full hp and mana.")
    (valid-params? [_ {:keys [source]}]
      source)
    (do! [_ {:keys [source]}]
      ; ...
      )
    )
