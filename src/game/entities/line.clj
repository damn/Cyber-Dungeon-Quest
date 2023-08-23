(ns game.entities.line
  (:require [game.db :as db]
            game.components.line-render))

(defn create! [& {:keys [start end duration color thick?]}]
  (println "Create line entity")
  (println "start " start " end" end " color" color "thick? " thick?)
  (assert color)
  (db/create-entity!
   {:position start
    :z-order :effect
    :line-render {:thick? thick?
                  :end end
                  :color color}
    :delete-after-duration duration}))
