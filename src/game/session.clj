(ns game.session
  (:require gdl.context
            gdl.disposable
            game.context)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.scenes.scene2d.Stage))

; TODO this all move to gdl
(extend-type gdl.context.Context
  game.context/Context
  (show-msg-to-player! [_ message]
    (println message))

  game.context/ContextStageSetter
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
  game.context/Stage
  (draw [stage]
    (.draw stage))
  (act [stage delta]
    (.act stage delta)))
; TODO also for my own Disposable protocol
