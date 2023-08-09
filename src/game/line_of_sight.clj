(nsx game.line-of-sight
  (:require [game.maps.cell-grid :as cell-grid]
            [game.player.entity :refer (player-entity)]))

; TODO ! transparent entities shows 'zzz'
; -> make them either visible or not ... ? not inbetween ?

; -> implement minimap by zooming out (remove the state, clamp )
; check movement possible by zooming
; -> no targeting something out of range (whats your range ?)

; -> moving around by mousing at screen borders
; -> until player moves and snaps you back
; -> nicely like strategy game ?

; use rectangle collider and make viewport rectangle
; -> all renderables (z-order) -> require a bounding-box -> no need for :in-line-of-sight
; for animations/effects & they can be mouseovered for debug
; -> but do not make status gui infotext/mouseover outline / attackable


#_(let [screen-frustum-rect {:left-bottom a
                             :width b
                             :height c}]
    (geom/collides?
     screen-frustum-rect
     rectangle)) ; rectangle == entity*

; TODO weird mixing up camera & player
(defn- on-screen? [entity*]
  (let [[x y] (:position entity*)
        x (float x)
        y (float y)
        [cx cy] (world/camera-position)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
      (<= xdist (inc (/ (world/viewport-width)  2)))
      (<= ydist (inc (/ (world/viewport-height) 2))))))

; TODO big entities (big effects, nova effects, > 1 width/height)
; => check on-screen with width&height and also with ray-blocked to all 4 corners
; to check if its in line of sight.

; could memoize it in the entities which entities(positions?) they are seeing or not
; and on position change delete it -> look up
; even for light sources could memoize position-to-position line of sight checks (static)
(defn in-line-of-sight? [source* target*]
  (and (:z-order target*)  ; is even an entity which renders something
       (or (not (:is-player source*))
           (on-screen? target*))
       (not (cell-grid/ray-blocked? (:position source*)
                                    (:position target*)))))

; TODO very slow do not use this for normal line-of-sight-checks
; only visible to player check... !
; make separate fn for not player ?!

; TODO ? check light value (at position, not corners)
; => one more raycast done -> if not lighted = not rendered
; but special case player is also lightsource ....
; - check inside light radius visibility (first check inside radius then check lightness? or just in radius . ?)
; - check body 4 corners lightness values > 0 (can pass it to renderfn 4 lightness values)

; => entities outside light radius are not rendered at all - and cannot be selected with mouseoverbody / targeted.

; -> maybe light-value nil or >0 assoce'd, can be used @ render

; -> visible-entities already could calculate for them all the light-color value
; for all corners
; and pass it with render-entity functions
; and if light-color < 0 then no need to call render for that entity AT ALL
; -> all sub-entities/effects will not be rendered if it has lightness value = 0
