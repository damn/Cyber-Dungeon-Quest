(nsx game.components.animation)

; could be a (defsystem tick-e [c e delta]) ?? reintroduce ? pure function?
(defn- assoc-frame! [r]
  (swap! r #(assoc % :image (animation/get-frame (:animation %)))))

; if animation itself would be a sub-component it could 'tick' itself.
; it would have a 'counter' ? or animation-is-a-counter ...
(defcomponent :animation animation
  (db/create! [_ e]
    (assoc-frame! e))
  (tick! [c e _delta]
    (assoc-frame! e))
  (tick [_c delta]
    (animation/update animation delta)))
