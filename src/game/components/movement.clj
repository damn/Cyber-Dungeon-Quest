(ns game.components.movement
  (:require [x.x :refer [defcomponent doseq-entity]]
            [gdl.math.geom :as geom]
            [gdl.math.vector :as v]
            [utils.core :refer [find-first]]
            [game.entity :as entity]
            [game.effect :as effect]
            [game.components.body :as body]
            [game.entities.audiovisual :as audiovisual]
            [game.maps.cell-grid :as grid]))

(defn- apply-delta-v [entity* delta v]
  (let [{:keys [speed]} (:speed entity*)
        ; TODO check max-speed (* speed multiplier delta )
        apply-delta (fn [p]
                      (mapv #(+ %1 (* %2 speed delta))
                            p
                            v))]
    (-> entity*
        (update :position    apply-delta)
        ; TODO on-position changed trigger make..
        (update-in [:body :left-bottom] apply-delta)))) ; TODO left-bottom/center of 'body' has on-position-changed?
; but need to check here ... lets see

(defn- update-position-projectile [{:keys [context/world-map] :as context} projectile delta v]
  (swap! projectile apply-delta-v delta v)
  (let [{:keys [hit-effects
                already-hit-bodies
                piercing]} (:projectile-collision @projectile)
        cell-grid (:cell-grid world-map)
        touched-cells (grid/rectangle->touched-cells cell-grid (:body @projectile))
        ; valid-position? for solid entity check
        ; on invalid returns {:hit-entities, :or :touched-cells}'
        hit-entity (find-first #(and (not (contains? already-hit-bodies %))
                                     (not= (:faction @projectile) (:faction @%))
                                     (:is-solid (:body @%))
                                     (geom/collides? (:body @projectile) (:body @%)))
                               (grid/get-entities-from-cells
                                touched-cells))
        blocked (cond hit-entity
                      (do
                       (swap! projectile update-in [:projectile-collision :already-hit-bodies] conj hit-entity)
                       (effect/do-all! hit-effects
                                       {:source projectile
                                        :target hit-entity}
                                       context)
                       (not piercing))
                      (some #(grid/cell-blocked? % @projectile) touched-cells)
                      (do
                       (audiovisual/create! context
                                            (:position @projectile)
                                            :projectile/hit-wall-effect)
                       true))]
    (if blocked
      (do
       (swap! projectile assoc :destroyed? true)
       false) ; not moved
      true))) ; moved

(defn- try-move [context entity delta v]
  (let [entity* (apply-delta-v @entity delta v)]
    (when (body/valid-position? context entity*)
      (reset! entity entity*)
      true)))

(defn- update-position-solid [context entity delta {vx 0 vy 1 :as v}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move context entity delta v)
        (try-move context entity delta [xdir 0])
        (try-move context entity delta [0 ydir]))))

; das spiel soll bei 20fps noch "schnell" sein,d.h. net langsamer werden (max-delta wirkt -> game wird langsamer)
; 1000/20 = 50
(def max-delta 50)

; max speed damit kleinere bodies beim direkten dr�berfliegen nicht �bersprungen werden (an den ecken werden sie trotzdem �bersprungen..)
; schnellere speeds m�ssten in mehreren steps sich bewegen.
(def ^:private max-speed (* 1000 (/ body/min-solid-body-size max-delta)))
; => world-units / second
; TODO also max speed limited by tile-size !! another max. value ! independent of min-solid-body size

; TODO check if speed > max speed, then make multiple smaller updates
; -> any kind of speed possible (fast arrows)

; TODO put movement-vector here also, make 'movement' component
; and further above 'body' component
(defcomponent :speed speed-in-seconds ; movement speed-in-seconds
  (entity/create! [[k _] e _ctx]
    (assert (and (:body @e)
                 (:position @e)))
    (swap! e assoc k {:speed (/ speed-in-seconds 1000)}))
  ; TODO make update w. movement-vector (add-component :movement-vector?)
  ; all assoc key to entity main map == add/remove component ?!
  ; how to do this with assoc/dissoc ?
  (entity/tick! [_ context e delta]
    ; TODO direction-vector not 'v' , 'v' is value
    (when-let [v (:movement-vector @e)]
      (assert (or (zero? (v/length v)) ; TODO what is the point of zero length vectors?
                  (v/normalised? v)))
      (when-not (zero? (v/length v))
        (when-let [moved? (if (:projectile-collision @e)
                            ; => move! called on body itself ???
                            (update-position-projectile context e delta v)
                            (update-position-solid      context e delta v))]
          (doseq-entity e entity/moved! context v))))))
