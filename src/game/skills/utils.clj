(ns game.skills.utils
  (:require [game.db :as db]
            [gdl.graphics.image :as image]
            [gdl.graphics.color :as color]))

(defn ^:private cross [position image]
  (db/create-entity!
   {:position position
    :in-line-of-sight true ; because used where not in sight f.e.
    :z-order :info ; TODO :info not implemented
    :image image
    :delete-after-duration 1000}))

(def ^:private old-cross (atom nil))

(defn- not-allowed-position-effect [position]
  (when (and @old-cross (db/exists? @old-cross))
    (swap! @old-cross assoc :destroyed? true))
  (reset! old-cross
          (cross position
                 (image/create "effects/forbidden.png" :transparent color/white :scale [32 32]))))
; TODO set white color to transparent in the effect picture

; TODO naming
#_(defn check-line-of-sight [entity] ; unused
  (let [target (get-skill-use-mouse-tile-pos)]
    (if (raycaster/ray-blocked? (:position @entity) target) ; TODO entity
      (do
        (show-msg-to-player "No line of sight to target!")
        (not-allowed-position-effect target)
        false)
      true)))
