(ns cdq.context.ui.player-modal
  (:require [gdl.context :refer [get-stage ->window ->label ->text-button add-to-stage!]]
            [gdl.scene2d.actor :refer [remove!]]
            cdq.api.context))

; TODO no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.

(defn- show-player-modal! [{:keys [gui-viewport-width gui-viewport-height] :as ctx}
                           {:keys [title text button-text on-click]}]
  (assert (not (::modal (get-stage ctx))))
  (add-to-stage! ctx
                 (->window ctx {:title title
                                :rows [[(->label ctx text)]
                                       [(->text-button ctx
                                                       button-text
                                                       (fn [ctx]
                                                         (remove! (::modal (get-stage ctx)))
                                                         (on-click ctx)))]]
                                :id ::modal
                                :modal? true
                                :center-position [(/ gui-viewport-width 2)
                                                  (* gui-viewport-height (/ 3 4))]
                                :pack? true})))

(defmethod cdq.api.context/transact! :tx/player-modal [[_ params] ctx]
  (show-player-modal! ctx params)
  nil)
