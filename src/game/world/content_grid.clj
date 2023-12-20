(ns game.world.content-grid)

(defprotocol ContentGrid
  (update-entity! [_ entity])
  (remove-entity! [_ entity]))
