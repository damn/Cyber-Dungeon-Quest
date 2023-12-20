(ns game.world.grid)

(defprotocol Grid
  (cached-adjacent-cells [_ cell])
  (rectangle->cells [_ rectangle])
  (circle->cells    [_ circle]))
