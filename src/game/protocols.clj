(ns game.protocols)

(defprotocol EntityComponentSystem
  (get-entity [_ id])
  (entity-exists? [_ entity])
  (create-entity! [_ components-map]
                  "Entities should not have :id component, will get added.
                  Calls entity/create system on the components-map
                  Then puts it into an atom and calls entity/create! system on all components.")
  (tick-active-entities [_ delta])
  (render-visible-entities [_ drawer])
  (destroy-to-be-removed-entities! [_]
                                   "Calls entity/destroy and entity/destroy! on all entities which are marked as ':destroyed?'"))

(defprotocol Context
  (play-sound! [_ file]
               "Sound is already loaded from file, this will perform only a lookup for the sound and play it.")
  (show-msg-to-player! [_ message]))

(defprotocol ViewRenderer
  (render-view [_ view-key render-fn]))

(defprotocol GameScreenTick
  (tick-game [_ stage delta])) ; TODO before-stage-tick ? ; TODO remove stage, put in context

(defprotocol GameScreenRender
  (render-world-map     [_])
  (render-in-world-view [_ drawer])
  (render-in-gui-view   [_ drawer]))

(defprotocol DebugRenderer
  (render-debug-before-entities [_ drawer])
  (render-debug-after-entities  [_ drawer]))

(defprotocol StageCreater
  (create-gui-stage [_ actors]))

(defprotocol ContextStageSetter
  (set-screen-stage [_ stage])
  (remove-screen-stage [_]))

(defprotocol Stage
  (draw [_])
  ; TODO this is 'tick!' or 'act!' because with side effects, different protocol than counter/animation
  (act [_ delta]))
