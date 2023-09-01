(ns game.screens.options
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.utils :refer [dispose]]
            [gdl.app :as app]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.image :as image]
            [gdl.graphics.batch :refer [batch]]
            [gdl.input :as input]
            [gdl.scene2d.ui :as ui]
            [utils.core :refer [find-first]]
            [game.session :as session]
            [game.ui.mouseover-entity :refer (show-entity-props-on-mouseover)]
            ;[game.line-of-sight :refer (player-line-of-sight-checks)]
            [game.components.body :refer (show-body-bounds)]
            [game.components.skills :refer (show-skill-icon-on-active)]))

; no protocol
(defprotocol StatusCheckBox
  (get-text [this])
  (get-state [this])
  (set-state [this is-selected]))

; TODO use vec not atom
(def status-check-boxes (atom []))

(defmacro status-check-box [& forms]
  `(swap! status-check-boxes conj
          (reify StatusCheckBox ~@forms)))

(defn- debug-flag [avar]
  (reify StatusCheckBox
    (get-text [this] (name (:name (meta avar))))
    (get-state [this] @avar)
    (set-state [this is-selected] (.bindRoot ^clojure.lang.Var avar is-selected))))

;(status-check-box
;  (get-text [this] "Music")
;  (get-state [this] (.isMusicOn app-game-container))
;  (set-state [this is-selected] (.setMusicOn app-game-container is-selected)))

; TODO
; * add: potential field (monster, player)
; * make separate DEBUG 'd' state , do not show this on options state
(def ^:private debug-flags [;(debug-flag #'player-line-of-sight-checks)
                            (debug-flag #'show-entity-props-on-mouseover)
                            (debug-flag #'show-body-bounds)
                            (debug-flag #'show-skill-icon-on-active)
                            ])

(status-check-box
  (get-text [this] "Sound")
  (get-state [this] #_(.isSoundOn app-game-container))
  (set-state [this is-selected] #_(.setSoundOn app-game-container is-selected)))

(status-check-box
  (get-text [this] "Fullscreen")
  (get-state [this] #_(.isFullscreen app-game-container))
  (set-state [this is-selected]
    #_(if (fullscreen-supported?)
      (.setFullscreen app-game-container is-selected))))

(status-check-box
  (get-text [this] "Show FPS")
  (get-state [this] #_(.isShowingFPS app-game-container))
  (set-state [this is-selected] #_(.setShowFPS app-game-container is-selected)))

(def state
  (reify session/State
    (load! [_ data]
      (if data
        (doseq [[text state] data
                :let [status-check-box (find-first
                                        #(= text (get-text %))
                                        @status-check-boxes)]]
          (set-state status-check-box state))))
    (serialize [_]
      (for [status @status-check-boxes]
        [(get-text status)
         (get-state status)]))
    (initial-data [_])))

(def ^:private exit #(app/set-screen :game.screens.ingame))

(defn- create-table []
  (let [table (ui/table :rows (concat
                               (for [check-box @status-check-boxes]
                                 [(ui/check-box (get-text check-box)
                                                #(set-state check-box %)
                                                (boolean (get-state check-box)))])
                               (for [check-box debug-flags]
                                 [(ui/check-box (get-text check-box)
                                                #(set-state check-box %)
                                                (boolean (get-state check-box)))])
                               [[(ui/text-button "Resume" exit)]
                                [(ui/text-button "Exit" #(app/set-screen :game.screens.main))]])
                        :fill-parent? true
                        :cell-defaults {:pad-bottom 25})
        padding 25]
    (.center table) ; ? TODO don't understand
    (def menu-bg-image (image/create "ui/moon_background.png"))
    table))

(defmodule stage
  (lc/create [_]
    (let [stage (ui/stage gui/viewport batch)]
      (.addActor stage (create-table))
      stage))
  (lc/dispose [_]
    (dispose stage))
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_]
    (gui/render
     (fn []
       (image/draw-centered menu-bg-image
                            [(/ (gui/viewport-width)  2)
                             (/ (gui/viewport-height) 2)])
       (ui/draw-stage stage batch))))
  (lc/tick [_ delta]
    (ui/update-stage stage delta)
    (when (input/is-key-pressed? :ESCAPE)
      (exit))))
