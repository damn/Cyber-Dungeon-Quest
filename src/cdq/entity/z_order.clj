(ns cdq.entity.z-order
  (:require [core.component :refer [defattribute]]
            [cdq.attributes :as attr]))

(defattribute :entity/z-order (attr/enum :z-order/on-ground
                                         :z-order/ground
                                         :z-order/flying
                                         :z-order/effect))
