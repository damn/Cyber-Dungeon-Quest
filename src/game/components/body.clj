(ns game.components.body
  (:require [x.x :refer [defsystem defcomponent update-map doseq-entity]]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.color :as color]
            [gdl.geom :as geom]
            [game.db :as db]
            [game.render :as render]
            [game.maps.cell-grid :as grid]))

(defn- remove-from-occupied-cells [e]
  (doseq [cell (:occupied-cells @e)]
    (swap! cell update :occupied disj e)))

(defn- set-occupied-cells [e]
  (let [cells (grid/rectangle->occupied-cells (:body @e))]
    (doseq [cell cells]
      (swap! cell update :occupied conj e))
    (swap! e assoc :occupied-cells cells)))

(defn- update-occupied-cells [e]
  (remove-from-occupied-cells e)
  (set-occupied-cells         e))

(defn- set-touched-cells
  ([e]
   (set-touched-cells e (grid/rectangle->touched-cells (:body @e))))
  ([e new-cells]
   {:pre [(not-any? nil? new-cells)]}
   (swap! e assoc :touched-cells new-cells)
   (doseq [cell new-cells]
     (grid/add-entity cell e))))

(defn- remove-from-touched-cells [e]
  (doseq [cell (:touched-cells @e)]
    (grid/remove-entity cell e)))

(defn update-touched-cells [e touched-cells]
  (when-not (= touched-cells (:touched-cells @e))
    (remove-from-touched-cells e)
    (set-touched-cells e touched-cells)))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
(def min-solid-body-size 0.3)

(defn- draw-bounds [{[x y] :left-bottom :keys [width height is-solid]}]
  (shape-drawer/rectangle x y width height (if is-solid color/white color/gray)))

(defn assoc-left-bottom [{:keys [body] [x y] :position :as entity*}]
  (assoc-in entity* [:body :left-bottom] [(- x (/ (:width body)  2))
                                          (- y (/ (:height body) 2))]))

(defn- body-props [position {:keys [width height is-solid]}]
  {:pre [position
         width
         height
         (>= width  (if is-solid min-solid-body-size 0))
         (>= height (if is-solid min-solid-body-size 0))]}
  {:half-width  (/ width  2)
   :half-height (/ height 2)
   :radius (max (/ width  2)
                (/ height 2))})

(defn valid-position?
  ([entity*]
   (valid-position? entity* (grid/rectangle->touched-cells (:body entity*))))
  ([entity* touched-cells]
   (and
    (not-any? #(grid/cell-blocked? % entity*) touched-cells)
    (or (not (:is-solid (:body entity*)))
        (->> touched-cells
             grid/get-entities-from-cells
             (not-any? #(and (not= (:id @%) (:id entity*))
                             (:is-solid (:body @%))
                             (geom/collides? (:body @%) (:body entity*)))))))))

(defsystem moved  [c direction-vector]) ; ! applied to body sub-components !
(defsystem moved! [c e])

(defn apply-moved-systems! [e direction-vector]
  (swap! e update :body update-map moved direction-vector)
  (doseq-entity e moved!))

(def show-body-bounds false)

(defcomponent :body {:keys [is-solid] :as body}
  (db/create! [[k _] e]
    (swap! e update k merge (body-props (:position @e) body))
    (swap! e assoc-left-bottom)
    (set-touched-cells e)
    (when is-solid
      (set-occupied-cells e)))
  (db/destroy! [_ e]
    (remove-from-touched-cells e)
    (when is-solid
      (remove-from-occupied-cells e)))
  (moved! [_ e]
    (assert (valid-position? @e))
    ; update-touched-cells done manually @ update-position
    (when is-solid
      (update-occupied-cells e)))
  (render/debug [c m p]
    (when show-body-bounds
      (draw-bounds body))))
