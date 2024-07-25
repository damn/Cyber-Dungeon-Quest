(ns cdq.context.ui.debug-window
  (:require [gdl.context :refer [frames-per-second mouse-on-stage-actor? ->actor ->window ->label]]
            [gdl.graphics :as g]
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
                            context/game-logic-frame]
                     {:keys [world-camera] :as g} :context/graphics
                     :as ctx}]
  (let [world-mouse (g/world-mouse-position g)]
    (str
     "game-logic-frame: " @game-logic-frame "\n"
     "FPS: " (frames-per-second ctx)  "\n"
     "Zoom: " (camera/zoom world-camera) "\n"
     "World: "(mapv int world-mouse) "\n"
     "X:" (world-mouse 0) "\n"
     "Y:" (world-mouse 1) "\n"
     "GUI: " (g/gui-mouse-position g) "\n"
     (when @thrown-error
       (str "\nERROR!\n " @thrown-error "\n\n"))
     "game-paused? " @game-paused? "\n"
     "elapsed-game-time " (utils.core/readable-number @elapsed-game-time) " seconds \n"
     (skill-info @player-entity)
     ;"\nMouseover-Actor:\n"
     #_(when-let [actor (mouse-on-stage-actor? ctx)]
         (str "TRUE - name:" (.getName actor)
              "id: " (gdl.scene2d.actor/id actor)
              )))))

(defn create [{{:keys [gui-viewport-height]} :context/graphics
               :as context}]
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
