(nsx game.screens.options
  (:require [game.ui.mouseover-entity :refer (show-entity-props-on-mouseover)]
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
    (set-state [this is-selected] (set-var-root avar is-selected))))

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

(def ^:private exit #(game/set-screen :ingame))

(ui/def-stage stage)

(app/on-create
 (def table (ui/table))
 (.setFillParent table true)
 (.addActor stage table)
 (.center table)
 (.setDebug table false)
 (def resume-button   (ui/text-button "Resume" exit))
 (def exit-button     (ui/text-button "Exit"   #(game/set-screen :mainmenu)))
 (def padding 25)
 (.padBottom (.add table resume-button) (float padding))
 (.row table)
 (.padBottom (.add table exit-button) (float padding))
 (.row table)
 (doseq [check-box @status-check-boxes
         :let [cb (ui/check-box (get-text check-box)
                                 #(set-state check-box %)
                                 (boolean (get-state check-box)))]]
   (.add table cb))
 (.row table)
 (doseq [check-box debug-flags
         :let [cb (ui/check-box (get-text check-box)
                                 #(set-state check-box %)
                                 (boolean (get-state check-box)))]]
   (.add table cb))

 (def menu-bg-image (image/create "ui/moon_background.png")))

(defn- render* []
  (image/draw-centered menu-bg-image
                       [(/ (g/viewport-width)  2)
                        (/ (g/viewport-height) 2)])
  (ui/draw-stage stage))

(game/defscreen options-screen
  :show (fn []
          (input/set-processor stage))
  :dispose (fn [])
  :render (fn []
            (g/render-gui render*))
  :update (fn [delta]
            (ui/update-stage stage delta)
            (when (input/is-key-pressed? :ESCAPE)
              (exit))))
