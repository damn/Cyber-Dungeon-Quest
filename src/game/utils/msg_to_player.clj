; MOVE TO UI !
(ns game.utils.msg-to-player
  (:require [utils.core :refer [assoc-in! update-in!]]
            [game.session :as session]))

; TODO use game.utils.counter instead
(defn- reset-counter! [counter]
  (assoc-in! counter [:current] 0))

(defn- update-counter
  "updates counter. if maxtime reached, resets current to 0 and returns the last current value,
   else returns nil."
  [counter delta]
  (update-in! counter [:current] + delta)
  (let [{current :current maxtime :max} @counter]
    (when (and maxtime (>= current maxtime))
      (let [last-current current]
        (reset-counter! counter)
        last-current))))

(def ^:private message (atom nil))
(def ^:private counter (atom nil))

(def state (reify session/State
             (load! [_ _]
               (reset! message nil)
               (reset! counter {:current 0,:max 4000}))
             (serialize [_])
             (initial-data [_])))

(defn show-msg-to-player [& more]
  (reset-counter! counter)
  (reset! message (apply str more)))

(defn update-msg-to-player [delta]
  (when (and @message (update-counter counter delta))
    (reset! message nil)))

(defn render-message-to-player []
  (when-let [msg @message]
    #_(render-readable-text (/ (gui/viewport-width) 2)
                            (/ (gui/viewport-height) 4)
                            {:centerx true :background false}
                            msg)))
