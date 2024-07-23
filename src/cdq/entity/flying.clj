(ns cdq.entity.flying
  (:require [x.x :refer [defattribute]]
            [cdq.attributes :as attr]))

(defattribute :entity/flying? attr/boolean-attr) ; optional, mixed with z-order
