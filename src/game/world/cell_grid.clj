(ns game.world.cell-grid)

(defprotocol CellGrid
  (cached-get-adjacent-cells [_ cell])
  (rectangle->touched-cells [_ rectangle])
  (circle->touched-cells    [_ circle]))
