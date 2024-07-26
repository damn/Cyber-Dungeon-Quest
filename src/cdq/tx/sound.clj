(ns cdq.tx.sound
  (:require [core.component :refer [defcomponent]]
            [gdl.context :refer [play-sound!]]
            [cdq.api.context :refer [transact!]]
            [cdq.attributes :as attr]))

(defcomponent :tx/sound attr/sound
  file
  (transact! [_ ctx]
    (play-sound! ctx file)
    nil))
