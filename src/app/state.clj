(ns app.state
  (:require [gdl.app :refer [change-screen]]))

(def current-context (atom nil))

(defn change-screen! [new-screen-key]
  (swap! current-context change-screen new-screen-key))
