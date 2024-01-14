(ns cdq.screens.options-menu
  (:require [gdl.app :refer [change-screen!]]
            gdl.screen
            [gdl.context :refer [->text-button ->check-box key-just-pressed? ->table]]
            [gdl.input.keys :as input.keys]
            [utils.core :refer [safe-get]]
            cdq.context.transaction-handler
            cdq.context.render-debug
            cdq.context.world
            cdq.screens.game
            cdq.entity.body))

(defprotocol StatusCheckBox
  (get-text [this])
  (get-state [this])
  (set-state [this is-selected]))

#_(def status-check-boxes (atom []))

#_(defmacro status-check-box [& forms]
  `(swap! status-check-boxes conj (reify StatusCheckBox ~@forms)))

#_(status-check-box
  (get-text [this] "Sound")
  (get-state [this] #_(.isSoundOn app-game-container))
  (set-state [this is-selected] #_(.setSoundOn app-game-container is-selected)))

(defn- ->debug-flag [avar]
  (reify StatusCheckBox
    (get-text [this]
      (let [m (meta avar)]
        (str "[LIGHT_GRAY]" (str (:ns m)) "/[WHITE]" (name (:name m)))))
    (get-state [this]
      @avar)
    (set-state [this is-selected]
      (.bindRoot ^clojure.lang.Var avar is-selected))))

; TODO add line of sight activate, shadows on/off, see through walls etc.
; TODO FIXME IF THE FLAGS ARE CHANGED MANUALLY IN THE REPL THIS IS NOT REFRESHED
(def ^:private debug-flags (map ->debug-flag
                                [#'cdq.entity.body/show-body-bounds
                                 ;#'cdq.context.transaction-handler/record-txs?
                                 #'cdq.context.transaction-handler/debug-print-txs?
                                 #'cdq.context.render-debug/tile-grid?
                                 #'cdq.context.render-debug/cell-occupied?
                                 #'cdq.context.render-debug/highlight-blocked-cell?
                                 #'cdq.context.render-debug/cell-entities?
                                 #'cdq.context.render-debug/potential-field-colors?
                                 #'cdq.screens.game/pausing?
                                 #'cdq.context.world/player-los-checks?
                                 #'cdq.context.world/see-all-tiles?
                                 #'cdq.context.world/spawn-enemies?]))

(defn- exit []
  (change-screen! :screens/game))

(defn- create-table [{:keys [context/config] :as ctx}]
  (->table ctx
           {:rows (concat
                   #_(for [check-box @status-check-boxes]
                       [(->check-box ctx
                                     (get-text check-box)
                                     #(set-state check-box %)
                                     (boolean (get-state check-box)))])
                   (when (safe-get config :debug-options?)
                     (for [check-box debug-flags]
                       [(->check-box ctx
                                     (get-text check-box)
                                     (partial set-state check-box)
                                     (boolean (get-state check-box)))]))
                   [[(->text-button ctx "Resume" (fn [_ctx] (exit)))]
                    [(->text-button ctx "Exit" (fn [_ctx] (change-screen! :screens/main-menu)))]])
            :fill-parent? true
            :cell-defaults {:pad-bottom 10}}))

(deftype SubScreen []
  gdl.screen/Screen
  (show [_ _ctx])
  (hide [_ _ctx])
  (render [_ ctx]
    (when (key-just-pressed? ctx input.keys/escape)
      (exit))))

(defn screen [ctx background-image]
  {:actors [background-image (create-table ctx)]
   :sub-screen (->SubScreen)})
