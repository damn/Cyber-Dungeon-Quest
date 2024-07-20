(ns cdq.tx.sound
  (:require [gdl.context :refer [play-sound!]]
            [cdq.api.context :refer [transact!]]))

(defmethod transact! :tx/sound [[_ file] ctx]
  (play-sound! ctx file)
  nil)
