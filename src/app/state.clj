(ns app.state
  (:require [gdl.context :refer [change-screen]]))

(def current-context (atom nil))

(defn change-screen!
  "change-screen is problematic, changes the current-context atom
  and then the frame finishes with the original unchanged context.
  do it always at end of frame"
  [new-screen-key]
  (swap! current-context change-screen new-screen-key))
