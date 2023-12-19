(ns game.session
  (:require gdl.context
            gdl.disposable
            [gdl.maps.tiled :as tiled]
            [gdl.scene2d.stage :as stage]
            context.ecs
            context.mouseover-entity
            game.protocols
            game.ui.action-bar
            game.ui.inventory-window)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.audio.Sound
           com.badlogic.gdx.scenes.scene2d.Stage))

; TODO this all move to gdl
(extend-type gdl.context.Context
  game.protocols/Context
  (play-sound! [{:keys [assets]} file]
    (.play ^Sound (get assets file)))

  (show-msg-to-player! [_ message]
    (println message))

  game.protocols/StageCreater
  (create-gui-stage [{:keys [gui-viewport batch]} actors]
    (let [stage (stage/create gui-viewport batch)]
      (doseq [actor actors]
        (.addActor stage actor))
      stage))

  game.protocols/ContextStageSetter
  (set-screen-stage [_ stage]
    ; set-screen-stage & also sets it to context ':stage' key
    (.setInputProcessor Gdx/input stage))
  (remove-screen-stage [_]
    ; TODO dissoc also :stage from context
    (.setInputProcessor Gdx/input nil)))

(extend-type Stage
  gdl.disposable/Disposable
  (dispose [stage]
    (.dispose stage))
  game.protocols/Stage
  (draw [stage]
    (.draw stage))
  (act [stage delta]
    (.act stage delta)))
; TODO also for my own Disposable protocol

; TODO this @ screns/main-menu
(defn init-context [context]
  (game.ui.inventory-window/rebuild-inventory-widgets!) ; before adding entities ( player gets items )
  ; TODO z-order namespaced keywords
  (let [context (merge context
                       (context.ecs/->context :z-orders [:on-ground ; items
                                                         :ground    ; creatures, player
                                                         :flying    ; flying creatures
                                                         :effect])  ; projectiles, nova
                       (context.mouseover-entity/->context-map)
                       (context.world-map/->context-map context)
                       {:context/running (atom true)})
        player-entity (create-entities-from-tiledmap! context)
        context (assoc context :context/player-entity player-entity)]
    (game.ui.action-bar/reset-skills!)
    context))
