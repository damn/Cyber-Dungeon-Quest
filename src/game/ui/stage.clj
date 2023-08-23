(nsx game.ui.stage
  ; also change @ x.intern
  (:require x.ns) ; one time require so clojure.tools.namespace knows the dependency order
  (:import com.badlogic.gdx.scenes.scene2d.Stage))

(declare ^Stage stage
         table)

(defmodule _
  (lc/create [_]
    (.bindRoot #'stage (ui/stage))
    (.bindRoot #'table (let [table (doto (ui/table)
                                     (.setFillParent true))]
                         (.addActor stage table)
                         table)))
  (lc/dispose [_]
    (.dispose stage)))

(defn mouseover-gui? []
  (ui/mouseover? stage))
