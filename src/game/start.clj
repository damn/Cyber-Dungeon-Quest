(ns game.start
  (:require [clojure.edn :as edn]
            [gdl.app :as app]
            [gdl.lc :as lc]
            [x.x :refer [defmodule]]
            [x.temp :refer [fire-event!]]))

(defmodule _
  (lc/create  [_] (fire-event! :app/create))
  (lc/dispose [_] (fire-event! :app/destroy)))

(defn app []
  (app/start (edn/read-string (slurp "resources/app_config.edn"))))
