(ns game.protocols) ; TODO == game.context

(defprotocol EntityComponentSystem
  (get-entity [_ id])
  (entity-exists? [_ entity])
  (create-entity! [_ components-map]
                  "Entities should not have :id component, will get added.
                  Calls entity/create system on the components-map
                  Then puts it into an atom and calls entity/create! system on all components.")
  (tick-active-entities [_ delta])
  (render-visible-entities [_])
  (destroy-to-be-removed-entities! [_]
                                   "Calls entity/destroy and entity/destroy! on all entities which are marked as ':destroyed?'"))

(defprotocol Context
  (play-sound! [_ file]
               "Sound is already loaded from file, this will perform only a lookup for the sound and play it.")
  (show-msg-to-player! [_ message]))

(defprotocol GameScreenTick
  (tick-game [_ stage delta])) ; TODO before-stage-tick ? ; TODO remove stage, put in context

(defprotocol GameScreenRender
  (render-world-map     [_])
  ; TODO confusing names with render-world-view / render-gui-view @ gdl.context
  (render-in-world-view [_])
  (render-in-gui-view   [_]))

(defprotocol DebugRenderer
  (render-debug-before-entities [_])
  (render-debug-after-entities  [_]))

(defprotocol StageCreater
  (create-gui-stage [_ actors]))

(defprotocol ContextStageSetter
  (set-screen-stage [_ stage])
  (remove-screen-stage [_]))

; TODO is just stage ... move to gdl
(defprotocol Stage
  (draw [_])
  ; TODO this is 'tick!' or 'act!' because with side effects, different protocol than counter/animation
  (act [_ delta]))

(defprotocol MouseOverEntity
  (update-mouseover-entity [_ stage-hit?]))
