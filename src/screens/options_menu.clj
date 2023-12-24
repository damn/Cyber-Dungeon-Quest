(ns screens.options-menu
  (:require [gdl.app :refer [change-screen!]]
            gdl.screen
            [gdl.context :refer [->stage-screen draw-centered-image render-gui-view create-image
                                 ->text-button ->check-box key-just-pressed? ->table]]
            [gdl.input.keys :as input.keys]
            [utils.core :refer [find-first]]
            ;[cdq.line-of-sight :refer (player-line-of-sight-checks)]
            [context.entity.body :refer (show-body-bounds)]))

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
                            (debug-flag #'show-body-bounds)
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

#_(def state
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

(defn- exit []
  (change-screen! :screens/game))

(defn- create-table [context]
  (let [table (->table context
                       {:rows (concat
                               (for [check-box @status-check-boxes]
                                 [(->check-box context
                                               (get-text check-box)
                                               #(set-state check-box %)
                                               (boolean (get-state check-box)))])
                               (for [check-box debug-flags]
                                 [(->check-box context
                                               (get-text check-box)
                                               #(set-state check-box %)
                                               (boolean (get-state check-box)))])
                               [[(->text-button context "Resume" (fn [_context] (exit)))]
                                [(->text-button context "Exit" (fn [_context]
                                                                 (change-screen! :screens/main-menu)))]])
                        :fill-parent? true
                        :cell-defaults {:pad-bottom 25}})
        padding 25]
    (.center table) ; :alignment :center ?
    (def menu-bg-image (create-image context "ui/moon_background.png"))
    table))

(defrecord SubScreen []
  gdl.screen/Screen
  (show [_ _ctx])
  (hide [_ _ctx])
  (render [_ {:keys [gui-viewport-width gui-viewport-height] :as context}]
    (render-gui-view context
                     (fn [c]
                       (draw-centered-image c
                                            menu-bg-image
                                            [(/ gui-viewport-width  2)
                                             (/ gui-viewport-height 2)])))
    (when (key-just-pressed? context input.keys/escape)
      (exit))))

(defn screen [context]
  {:actors [(create-table context)]
   :sub-screen (->SubScreen)})
