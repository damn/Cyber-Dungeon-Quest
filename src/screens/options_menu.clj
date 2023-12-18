(ns screens.options-menu
  (:require [gdl.lifecycle :as lc]
            [gdl.app :as app]
            [gdl.graphics.draw :as draw]
            [gdl.graphics.image :as image]
            [gdl.scene2d.ui :as ui]
            [gdl.scene2d.stage :as stage]
            [utils.core :refer [find-first]]
            ;[game.line-of-sight :refer (player-line-of-sight-checks)]
            [game.components.body :refer (show-body-bounds)])
  (:import (com.badlogic.gdx Gdx Input$Keys)
           com.badlogic.gdx.scenes.scene2d.Stage))

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

(def ^:private exit #(app/change-screen! :screens/game))

(defn- create-table [context]
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
                                [(ui/text-button "Exit" #(app/change-screen! :screens/main-menu))]])
                        :fill-parent? true
                        :cell-defaults {:pad-bottom 25})
        padding 25]
    (.center table) ; ? TODO don't understand
    (def menu-bg-image (image/create context "ui/moon_background.png"))
    table))

(defrecord Screen [^Stage stage]
  lc/Disposable
  (lc/dispose [_]
    (.dispose stage))
  lc/Screen
  (lc/show [_ _ctx]
    (.setInputProcessor Gdx/input stage))
  (lc/hide [_ _ctx]
    (.setInputProcessor Gdx/input nil))
  (lc/render [_ {:keys [gui-viewport-width gui-viewport-height] :as context}]
    (app/render-view context
                     :gui
                     (fn [drawer]
                       (draw/centered-image drawer
                                            menu-bg-image
                                            [(/ gui-viewport-width  2)
                                             (/ gui-viewport-height 2)])))
    (.draw stage))
  (lc/tick [_ _state delta]
    (.act stage delta)
    (when (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE)
      (exit))))

(defn screen [{:keys [gui-viewport batch] :as context}]
  (let [stage (stage/create gui-viewport batch)]
    (.addActor stage (create-table context))
    (->Screen stage)))
