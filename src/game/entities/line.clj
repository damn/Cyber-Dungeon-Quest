(ns game.entities.line
  (:require [game.context :as gm]))

(defn create! [context {:keys [start end duration color thick?]}]
  (gm/create-entity! context
                     {:position start
                      :z-order :effect
                      :line-render {:thick? thick?
                                    :end end
                                    :color color}
                      :delete-after-duration duration}))
