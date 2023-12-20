(ns game.world.cell-grid)

(defprotocol CellGrid
  (cached-adjacent-cells [_ cell])
  (rectangle->cells [_ rectangle])
  (circle->cells    [_ circle]))
