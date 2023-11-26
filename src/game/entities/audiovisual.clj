(ns game.entities.audiovisual
  (:require [x.x :refer [defmodule]]
            [gdl.audio :as audio]
            [gdl.lc :as lc]
            [utils.core :refer [safe-get]]
            [game.db :as db]
            [game.media :as media]))

; TODO move everything to properties.edn later
; TODO add shield-blocked-effect & armor-blocked-effect
; TODO no '-effect' necessary? but need later at properties?
(defn- create-properties []
  {:effects.damage.physical/hit-effect {:sound "bfxr_normalhit.wav"
                                        :animation (media/fx-impact-animation [3 0])}
   :effects.damage.magic/hit-effect {:sound "bfxr_curse.wav"
                                     :animation (media/fx-impact-animation [6 1])}
   :effects.target-entity/hit-ground-effect {:sound "bfxr_fisthit.wav"
                                             :animation (media/fx-impact-animation [0 1])}
   :creature/die-effect {:sound "bfxr_defaultmonsterdeath.wav"
                         :animation (media/blood-animation)}
   :projectile/hit-wall-effect {:sound "bfxr_projectile_wallhit.wav"
                                :animation (media/plop-animation)}
   })

(declare ^:private properties)

(defmodule _
  (lc/create [_]
    (.bindRoot #'properties (create-properties))))

(defn create! [position id]
  ; TODO not looping :pre ?  otherwise doesnt stop ?
  (let [{:keys [sound animation]} (safe-get properties id)]
    (audio/play sound)
    (db/create-entity!
     {:position position
      :animation animation
      :z-order :effect
      :delete-after-animation-stopped? true})))
