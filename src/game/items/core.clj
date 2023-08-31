(ns game.items.core
  (:require [clojure.string :as str]
            [gdl.graphics.color :as color]
            [game.components.modifiers :as modifiers]
            [game.player.entity :refer (player-entity)]))

; diablo2 unique gold rgb 144 136 88
(color/defrgb ^:private gold-item-color 0.84 0.8 0.52)
; diablo2 blue magic rgb 72 80 184
(color/defrgb modifiers-text-color 0.38 0.47 1)

; TODO write 'two handed' at weapon info -> key-to-pretty-tooltip-text function for keywords (extend-c?)
; TODO showing 'Sword' -> 'Sword' modifier -> stupid

(defn- item-name [item]
  (str (:pretty-name item)
       (when-let [cnt (:count item)]
         (str " (" cnt ")"))))

(defn text [item]
  (str (if (= (:slot item) :weapon)
         "" ; already in first modifier name of weapon skill == item name (also pretty-name?!)
         (str (item-name item) "\n"))
       ; TODO not player-entity but referenced entity, TODO only used @ skill modifier
       ; no need for this here ?
       (str/join "\n"
                 (for [modifier (:modifiers item)]
                   (modifiers/text player-entity modifier)))))
