(ns cdq.context.ui.debug-window
  (:require [gdl.context :refer [gui-mouse-position world-mouse-position frames-per-second
                                 mouse-on-stage-actor? ->actor ->window ->label]]
            [gdl.graphics.camera :as camera]
            [gdl.scene2d.group :refer [add-actor!]]
            [gdl.scene2d.ui.label :refer [set-text!]]
            [gdl.scene2d.ui.widget-group :refer [pack!]]))

(defn- skill-info [{:keys [entity/skills]}]
  (clojure.string/join "\n"
                       (for [{:keys [property/id skill/cooling-down?]} (vals skills)
                             :when cooling-down? ]
                         [id [:cooling-down? (boolean cooling-down?)]])))

(defn- debug-infos [{:keys [context/game-paused?
                            context/player-entity
                            cdq.context.ecs/thrown-error
                            context/elapsed-game-time
                            context/game-logic-frame
                            world-camera] :as c}]
  (let [world-mouse (world-mouse-position c)]
    (str
     "game-logic-frame: " @game-logic-frame "\n"
     "FPS: " (frames-per-second c)  "\n"
     "Zoom: " (camera/zoom world-camera) "\n"
     "World: "(mapv int world-mouse) "\n"
     "X:" (world-mouse 0) "\n"
     "Y:" (world-mouse 1) "\n"
     "GUI: " (gui-mouse-position c) "\n"
     (when @thrown-error
       (str "\nERROR!\n " @thrown-error "\n\n"))
     "game-paused? " @game-paused? "\n"
     "elapsed-game-time " (utils.core/readable-number @elapsed-game-time) " seconds \n"
     (skill-info @player-entity)
     ;"\nMouseover-Actor:\n"
     #_(when-let [actor (mouse-on-stage-actor? c)]
         (str "TRUE - name:" (.getName actor)
              "id: " (gdl.scene2d.actor/id actor)
              )))))

(defn create [{:keys [gui-viewport-height] :as context}]
  (let [label (->label context "")
        window (->window context {:title "Debug"
                                  :id :debug-window
                                  :visible? false
                                  :position [0 gui-viewport-height]
                                  :rows [[label]]})]
    (add-actor! window (->actor context
                                {:act
                                 #(do
                                   (set-text! label (debug-infos %))
                                   (pack! window))}))
    window))
