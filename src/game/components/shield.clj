(ns game.components.shield
  (:require [game.utils.counter :refer :all]))

#_(def- rotation-speed (/ 360 2000)) ; 360 degrees in 2 seconds = 2000 ms

#_(defctypefn :update-entity* :shield [{{:keys [is-active]} :shield :as entity*} delta]
  (if is-active
    (update-in entity* [:shield :angle] v/degree-add (* delta rotation-speed))
    (update entity*
            :shield
            #(update-finally-merge % :counter delta {:angle 0 :is-active true}))))

#_(defctypefn :render-above :shield [{{:keys [image angle is-active]} :shield} position]
  (when is-active
    (draw-rotated-centered-image image angle position)))

#_(defn shield-component [regeneration-duration]
  {:shield {:is-active true
            :angle 0
            :counter (make-counter regeneration-duration)
            :image (create-image "effects/shield.png")}})

; -> make just a component on the entity
#_(defeffectentity ^:private shield-hit [body]
  :target body
  :duration 100
  (circle-around-body-render-comp body (color 0.5 1 0 0.5) :air))

#_(defn shield-try-consume-damage [body]
  (if-let [shield (:shield @body)]
    (when (:is-active shield)
      (assoc-in! body [:shield :is-active] false)
      #_(shield-hit body)
      true)))
