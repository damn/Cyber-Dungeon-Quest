(ns cdq.entity.flying
  (:require [core.component :refer [defattribute]]
            [cdq.attributes :as attr]))

(defattribute :entity/flying? attr/boolean-attr) ; optional, mixed with z-order
