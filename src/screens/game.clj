(ns screens.game
  (:require [gdl.lifecycle :as lc]
            [gdl.scene2d.stage :as stage]
            game.ui.actors
            game.render
            game.tick)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.scenes.scene2d.Stage))

(defrecord IngameScreen [^Stage stage]
  lc/Disposable
  (lc/dispose [_]
    (.dispose stage))
  lc/Screen
  (lc/show [_ _ctx]
    (.setInputProcessor Gdx/input stage))
  (lc/hide [_]
    (.setInputProcessor Gdx/input nil))
  (lc/render [_ context]
    (game.render/render-game context)
    (.draw stage))
  (lc/tick [_ context delta]
    (game.tick/tick-game context stage delta)
    (.act stage delta)))

(defn screen [{:keys [gui-viewport batch] :as context}]
  (->IngameScreen (let [stage (stage/create gui-viewport batch)]
                    (doseq [actor (game.ui.actors/create-actors context)]
                      (.addActor stage actor))
                    stage)))
