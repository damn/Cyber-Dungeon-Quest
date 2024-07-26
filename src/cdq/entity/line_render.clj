(ns cdq.entity.line-render
  (:require [core.component :refer [defcomponent]]
            [gdl.graphics :as g]
            [cdq.api.entity :as entity]))

(defcomponent :entity/line-render {}
  {:keys [thick? end color]}
  (entity/render-default [_ {:keys [entity/position]} g _ctx]
    (if thick?
      (g/with-shape-line-width g 4
        #(g/draw-line g position end color))
      (g/draw-line g position end color))))
