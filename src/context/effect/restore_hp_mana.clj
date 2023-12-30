(ns context.effect.restore-hp-mana
  (:require [data.val-max :refer [lower-than-max? set-to-max]]
            [context.effect :as effect]))

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
  (swap! source #(-> %
                     (update :hp set-to-max)
                     (update :mana set-to-max))))
