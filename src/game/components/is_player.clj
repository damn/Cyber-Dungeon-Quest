(ns game.components.is-player
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.camera :as camera]
            [context.ecs :as entity]))

(defcomponent :is-player _
  (entity/create! [_ entity {:keys [world-camera]}]
    (camera/set-position! world-camera (:position @entity)))
  ; TODO make on position changed trigger
  (entity/tick! [_ {:keys [world-camera]} entity delta]
    (camera/set-position! world-camera (:position @entity))))
