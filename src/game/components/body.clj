(nsx game.components.body
  (:require game.components.delete-after-duration
            game.components.body.rotation-angle
            [game.components.render :refer (animation-entity)]
            ; TODO just make :as cell and cell/add, etc.
            [game.maps.cell-grid :refer (rectangle->occupied-cells
                                         rectangle->touched-cells
                                         add-entity
                                         remove-entity
                                         get-cell
                                         cell-blocked?
                                         get-entities-from-cells
                                         cached-get-adjacent-cells)]))


; body & movement & projectile-entity
; -> make a hook @ movement and move projectile code out
; return hit-entities/cells or valid

; TODO define occupied == only a center position of solid? body entity
; on that cell (multiple can occupy one cell .)
; check ...

(defn- remove-from-occupied-cells [r]
  (doseq [cell (:occupied-cells @r)]
    (swap! cell update :occupied disj r)))

(defn- set-occupied-cells [r]
  (let [cells (rectangle->occupied-cells (:body @r))]
    (doseq [cell cells]
      (swap! cell update :occupied conj r))
    (swap! r assoc :occupied-cells cells)))

; moved! operates on entity reference, but we only need body
; -> store occupied/touched-cells inside body?
; but how can I make a transaction on both cells&only body-sub-component (not full entity?)
; I also dont want to see occupied/touched-cells in entity data overview...
; can I do this without datomic ?
; it would return a new body ?? 'AND' have side effects .... that would be weird ??
; 'half' pure ? also ok?

; => moved! returns a . new body (pure) and operates on cells (impure ) => together!
; but touched-cells not here ! fix that first

(defn- update-occupied-cells [r]
  (remove-from-occupied-cells r)
  (set-occupied-cells         r))

; called @ create/destroy/change position for :is-solid :body

; TODO a occupied-cell system which operates on body component and is-solid ?
; @ cell-grid ns ?
; => just make create!destroy!position-changed! fns 'update-cell-grid-connections!'

(defn- set-touched-cells
  ([r]
   (set-touched-cells r (rectangle->touched-cells (:body @r))))
  ([r new-cells]
   {:pre [(not-any? nil? new-cells)]}
   (swap! r assoc :touched-cells new-cells)
   (doseq [cell new-cells]
     (add-entity cell r))))

(defn- remove-from-touched-cells [r]
  (doseq [cell (:touched-cells @r)]
    (remove-entity cell r)))

; TODO test if it works
(defn- update-touched-cells [r touched-cells]
  (when-not (= touched-cells (:touched-cells @r))
    (remove-from-touched-cells r)
    (set-touched-cells r touched-cells)))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
(def min-solid-body-size 0.3)

(def show-body-bounds false)

; TODO keep 'badlogic.geom.rectangle' in entity ? no conversions ?
(defn- render-body-bounds [{[x y] :left-bottom :keys [width height is-solid]}]
  (when show-body-bounds ; TODO allow editor/tool to deactive cfns ! all systems/cfns overview.
    ; TODO pass directly the 'body' as a rectangle here !
    (shape-drawer/rectangle x y width height (if is-solid color/white color/gray))))

(defn assoc-left-bottom [{:keys [body] [x y] :position :as entity*}]
  (assoc-in entity* [:body :left-bottom] [(- x (/ (:width body)  2))
                                          (- y (/ (:height body) 2))]))

; TODO make defrecord .
(defn- body-props [position {:keys [width height is-solid]}]
  {:pre [position
         width
         height
         (>= width  (if is-solid min-solid-body-size 0))
         (>= height (if is-solid min-solid-body-size 0))]}
  {:half-width  (/ width  2) ; TODO float?
   :half-height (/ height 2)
   :radius (max (/ width  2)
                (/ height 2))})

; comparing entity-id, but would be enough body-id also!
; if body would have an id (with datomic would have !)
(defn valid-position?
  ([entity*]
   (valid-position? entity* (rectangle->touched-cells (:body entity*))))
  ([entity* touched-cells]
   (and
    (not-any? #(cell-blocked? % entity*) touched-cells)
    (or (not (:is-solid (:body entity*)))
        (->> touched-cells
             get-entities-from-cells
             (not-any? #(and (not= (:id @%) (:id entity*))
                             (:is-solid (:body @%))
                             (geom/collides? (:body @%) (:body entity*)))))))))

; on-create/destroy/position changed:
; * cell-grid/entity update (all in body component)
; * contentfields update

; on-create/destroy:
; * id-entity-map update

; I want to extend the body component @ cell-grid
; without the body component knowing

(defcomponent :body {:keys [is-solid] :as body}

  ; TODO assert height is there
  (create! [[k _] e]
    (swap! e update k merge (body-props (:position @e) body))
    (swap! e assoc-left-bottom) ; TODO move in above fn
    (set-touched-cells e)
    (when is-solid
      (set-occupied-cells e)))

  (destroy! [_ e]
    (remove-from-touched-cells e)
    (when is-solid
      (remove-from-occupied-cells e)))

  (moved! [_ e]
    (assert (valid-position? @e))
    ; TODO update-touched-cells done manually @ update-position (FIXME)
    (when is-solid
      (update-occupied-cells e)))

  (render-debug [c m p]
    (render-body-bounds body)))

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

(defn- update-position-projectile [projectile delta v]
  (swap! projectile apply-delta-v delta v)
  (let [{:keys [hit-effects
                already-hit-bodies
                piercing
                hits-wall-effect]} (:projectile-collision @projectile)
        touched-cells (rectangle->touched-cells (:body @projectile))
        ; valid-position? for solid entity check
        ; on invalid returns {:hit-entities, :or :touched-cells}'
        hit-entity (find-first #(and (not (contains? already-hit-bodies %))
                                     (not= (:faction @projectile) (:faction @%))
                                     (:is-solid (:body @%))
                                     (geom/collides? (:body @projectile) (:body @%)))
                               (get-entities-from-cells
                                touched-cells))
        blocked (cond hit-entity
                      (do
                       (update-in! projectile [:projectile-collision :already-hit-bodies] conj hit-entity)
                       (effects/do-effects! {:source projectile
                                             :target hit-entity}
                                            hit-effects)
                       (not piercing))
                      (some #(cell-blocked? % @projectile) touched-cells)
                      (do
                       (hits-wall-effect (:position @projectile))
                       true))]
    (if blocked
      (do
       (swap! projectile assoc :destroyed? true)
       false) ; not moved
      (do
       (update-touched-cells projectile touched-cells)
       true)))) ; moved


(defn- try-move [entity delta v]
  (let [entity* (apply-delta-v @entity delta v)
        touched-cells (rectangle->touched-cells (:body entity*))]
    (when (valid-position? entity* touched-cells)
      (reset! entity entity*)
      (update-touched-cells entity touched-cells)
      true)))

(defn- update-position-solid [entity delta {vx 0 vy 1 :as v}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move entity delta v)
        (try-move entity delta [xdir 0])
        (try-move entity delta [0 ydir]))))

; TODO probably put somewhere else
; das spiel soll bei 20fps noch "schnell" sein,d.h. net langsamer werden (max-delta wirkt -> game wird langsamer)
; 1000/20 = 50
(def max-delta 50)

; max speed damit kleinere bodies beim direkten dr�berfliegen nicht �bersprungen werden (an den ecken werden sie trotzdem �bersprungen..)
; schnellere speeds m�ssten in mehreren steps sich bewegen.
(def ^:private max-speed (* 1000 (/ min-solid-body-size max-delta)))
; => world-units / second
; TODO also max speed limited by tile-size !! another max. value ! independent of min-solid-body size

; TODO check if speed > max speed, then make multiple smaller updates
; -> any kind of speed possible (fast arrows)

(defn apply-moved-systems! [e direction-vector]
  ; systems could apply to sub-components , specified in systems definition
  (swap! e update :body update-map moved direction-vector)
  (doseq-entity e moved!))

; TODO put movement-vector here also, make 'movement' component
; and further above 'body' component
(defcomponent :speed speed-in-seconds ; movement speed-in-seconds

  (create! [[k _] e]
    (swap! e assoc k {:speed (/ speed-in-seconds 1000)}))
                           ; :direction v !


  ; TODO make update w. movement-vector (add-component :movement-vector?)
  ; all assoc key to entity main map == add/remove component ?!
  ; how to do this with assoc/dissoc ?
  (tick! [_ e delta]
    ; TODO direction-vector not 'v' , 'v' is value
    (when-let [v (:movement-vector @e)]
      (assert (or (zero? (v/length v)) ; TODO what is the point of zero length vectors?
                  (v/normalised? v)))
      (when-not (zero? (v/length v))
        (let [moved? (if (:projectile-collision @e)
                       ; => move! called on body itself ???
                       (update-position-projectile e delta v)
                       (update-position-solid      e delta v))]
          (when moved?
            (apply-moved-systems! e v)))))))

; solid or projectile ?
; two types of bodies ???
; what does is-solid mean ?
; projectiles also hitting walls

; wait! can I apply 'moved!' systems also only on body or do I need full entity reference?

(defn- plop [position]
  (animation-entity
    :position position
    :animation (media/plop-animation)))

; TODO maxrange ?
; TODO make only common fields here
(defn fire-projectile
  [& {:keys [position
             faction
             size
             animation
             movement-vector
             hit-effects
             speed
             maxtime
             piercing]}]
  (create-entity! ; -> entities/projectile
   {:position position
    :faction faction

    :body {:width size
           :height size
           :is-solid false
           :rotation-angle 0}

    :z-order :effect ; body ?

    ; movement
    :speed speed
    :movement-vector movement-vector

    :animation animation

    :delete-after-duration maxtime

    :projectile-collision {:piercing piercing
                           :hit-effects hit-effects
                           :already-hit-bodies #{}
                           ; TODO not necessary in the data
                           :hits-wall-effect (fn [posi]
                                               (audio/play "bfxr_projectile_wallhit.wav")
                                               (plop posi))}}))

;; Teleporting

; TODO adjacent-cells could be nil -> :middle @cell -> NPE
#_(defn- find-nearby-valid-location [entity posi]
    ; todo left-bottom is in body now.
  (when-let [cell (find-first #(valid-position? (assoc-in @entity [:body :left-bottom] (:position @%)))
                              (cached-get-adjacent-cells (get-cell posi)))]
    (:middle @cell)))

(defn teleport
  "searches nearby positions for a free one if target is blocked
   if none exists -> just teleports to the blocked position."
  [entity posi]
  ; annoying I have to calculate left-bottom ... is there a shortcut to add
  ; left-bottom & position to entity* ?
  ;

  #_(change-position! entity
                    (if (valid-position? entity) ; TODO assoc entity* positions
                      posi
                      (if-let [valid (find-nearby-valid-location entity posi)]
                        valid
                        posi)))

  ; TODO here also ! 3 times called ! DRY
  ; (call-on-position-changed-triggers entity)
  )
