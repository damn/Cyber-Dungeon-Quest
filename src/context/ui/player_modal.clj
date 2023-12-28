(ns context.ui.player-modal
  (:require [gdl.context :refer [get-stage ->window ->label ->text-button]]
            [gdl.scene2d.actor :refer [remove!]]
            [gdl.scene2d.group :refer [add-actor!]]
            cdq.context))

; TODO no window movable type cursor appears here like in player idle
; TODO id not unique ... check not adding more than one / check on add-actor! ?
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?

; TODO put center middle top (layouting with/without table?)

(extend-type gdl.context.Context
  cdq.context/PlayerModal
  (show-player-modal! [ctx {:keys [title text button-text on-click]}]
    (add-actor! (get-stage ctx)
                (->window ctx {:title title
                               :rows [[(->label ctx text)]
                                      [(->text-button ctx
                                                      button-text
                                                      (fn [ctx]
                                                        (remove! (:player-modal (get-stage ctx)))
                                                        (on-click ctx)))]]
                               :id :player-modal
                               :modal? true
                               :center? true
                               :pack? true}))))
