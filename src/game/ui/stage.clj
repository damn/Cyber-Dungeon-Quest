(nsx game.ui.stage
  ; also change @ x.intern
  (:require x.ns)) ; one time require so clojure.tools.namespace knows the dependency order

(comment


 (declare ^:dispose ^Skin skin)

 (defn create-skin
   ([]
    (create-skin default-skin))
   ([skin]
    (set-var-root #'skin skin)))

 ; TODO but how to arrange it that
 ; it will be created before other on-create??
 ; by leaving it declared only yes
 ; ...



 ; add my font to skin
 ; -> need to do before any things get created with the skin
 ; for example game/items/inventory on-create ... ui/window ...
 ; => how to set the skin before anything gets loaded ?

 (let [empty-skin (Skin.)]
   (.add skin "font" my-font)
   ; skin.addRegion(new TextureAtlas(Gdx.files.internal("mySkin.atlas")));
   ; skin.load(Gdx.files.internal("mySkin.json"));
   ; TODO will this overload 'default-font' ?
   ; => I need to pass my custom skin to gdl.ui !
   ; then, in your JSON file you can reference “font”
   ;
   ; {
   ;   font: font
   ; }

   )
 )

(ui/def-stage stage)

(app/on-create

 ; TODO here load your custom skin and dispose the other one @ gdl.ui ?
 ; or create-skin needs to be called (better !)
 ;  before using ui stuff

 ;(ui/create-skin)

 )

(app/defmanaged table (let [table (doto (ui/table)
                                    (.setFillParent true))]
                        (.addActor stage table)
                        table))

(defn mouseover-gui? []
  (let [[x y] (g/mouse-coords)]
    (.hit stage x y true)))

; TODO search through game on-create (defmanaged)
