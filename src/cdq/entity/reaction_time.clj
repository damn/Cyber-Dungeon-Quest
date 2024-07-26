(ns cdq.entity.reaction-time
  (:require [core.component :refer [defattribute]]
            [cdq.attributes :as attr]))

(defattribute :entity/reaction-time attr/pos-attr)
