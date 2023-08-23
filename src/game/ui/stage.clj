(nsx game.ui.stage
  ; also change @ x.intern
  (:require x.ns) ; one time require so clojure.tools.namespace knows the dependency order
  (:import com.badlogic.gdx.scenes.scene2d.Stage))

; TODO move to screens/ingame, but also used at 1 place
; => mouseover-entity ! (mouseover-gui?)
; => pass stage as arg !
(app/defmanaged ^:dispose ^Stage stage (ui/stage))

(app/defmanaged table (let [table (doto (ui/table)
                                    (.setFillParent true))]
                        (.addActor stage table)
                        table))

(defn mouseover-gui? []
  (let [[x y] (gui/mouse-position)]
    (.hit stage x y true)))
