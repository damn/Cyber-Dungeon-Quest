(ns effect.restore-hp-mana
  (:require [gdl.context :refer [play-sound!]]
            [data.val-max :refer [lower-than-max? set-to-max]]
            [context.effect-interpreter :as effect]))

; TODO make with 'target' then can use as hit-effect too !
; ==> choose self or allies (or enemies)

(defmethod effect/useful? :effect/restore-hp-mana
  [{:keys [effect/source]} _effect]
  (or (lower-than-max? (:mana @source))
      (lower-than-max? (:hp   @source))))

(defmethod effect/text :effect/restore-hp-mana
  [_context _effect]
  "Restores full hp and mana.")

(defmethod effect/valid-params? :effect/restore-hp-mana
  [{:keys [effect/source]} _effect]
  source)

(defmethod effect/do! :effect/restore-hp-mana
  [{:keys [effect/source] :as context} _effect]
  (play-sound! context "sounds/bfxr_drugsuse.wav")
  (swap! source #(-> %
                     (update :hp set-to-max)
                     (update :mana set-to-max))))
