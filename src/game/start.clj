(ns game.start
  (:require [clojure.edn :as edn]
            [gdl.app :as app]))

(defn app []
  (app/start (edn/read-string (slurp "resources/app_config.edn"))))
