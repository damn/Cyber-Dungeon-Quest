(ns x.ns
  (:require [clojure.string :as str]
            (x [x       :refer :all]
               [db      :refer :all] ; TODO make :as db
               [systems :refer :all]
               [temp        :as app]
               [session :as session]) ; TODO session also a system ! on db component !!!
            (gdl ;[utils :refer :all]
                 app
                 [game      :as game]
                 [input     :as input]
                 [audio     :as audio]
                 [geom      :as geom]
                 [vector    :as v]
                 [ui        :as ui]
                 [tiled     :as tiled]
                 [raycaster :as raycaster]
                 [graphics  :as g])
            (gdl.graphics [color        :as color]
                          [shape-drawer :as shape-drawer]
                          [image        :as image]
                          [animation    :as animation]
                          [gui :as gui]
                          [world :as world]
                          [font :as font]
                          )
            ;; TODO what to do with those ???
            [utils.core :refer :all]
            [game.media :as media]
            [game.components.faction :as faction]
            [game.components.modifiers :as modifiers]
            [game.effects.core :as effects]))

; TODO whatever is here from game, move to 'x'/'gdx'?
; [game.utils.random :as random]
; game counter ??

; => TODO all x.* and gdl.* ! grep!


; Candidates:
; [game.maps.cell-grid :as cell-grid]
; [gdl.input :refer :all]
; [game.ui.mouseover-entity :refer (saved-mouseover-entity get-mouseover-entity)]
; [game.utils.counter :refer :all]
; [game.utils.msg-to-player :refer (show-msg-to-player)] (the println for games)
;
; [game.maps.cell-grid :as cell-grid]
;
; [game.line-of-sight :refer (in-line-of-sight?)]




; TODO import to clojure.core only minimal (ns+,nsx)
; or fully resolve ns+, only nsx ?


; * mouseover-entity
; * player-entity ?

; * counter / make-counter ???
; [game.utils.counter :as counter]
; why

; :delete-after-duration is-a :counter !

; game.components.modifiers
; => not a component !
; a system ?
; [game.utils.random :as rand]
