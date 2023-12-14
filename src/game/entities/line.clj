(ns game.entities.line
  (:require [game.db :as db]))

(defn create! [context {:keys [start end duration color thick?]}]
  (db/create-entity! context
                     {:position start
                      :z-order :effect
                      :line-render {:thick? thick?
                                    :end end
                                    :color color}
                      :delete-after-duration duration}))
