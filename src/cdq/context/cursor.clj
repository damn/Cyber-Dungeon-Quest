(ns cdq.context.cursor
  (:require [gdl.context :refer [->cursor]]
            [utils.core :refer [safe-get mapvals]]
            cdq.api.context))

(extend-type gdl.context.Context
  cdq.api.context/Cursor
  (set-cursor! [{:keys [context/cursors] :as ctx} cursor-key]
    (gdl.context/set-cursor! ctx (safe-get cursors cursor-key))))

(def ^:private cursors
  {:cursors/default ["default" 0 0]
   :cursors/black-x ["black_x" 0 0]
   :cursors/denied ["denied" 16 16]
   :cursors/hand-grab ["hand003" 4 16]
   :cursors/hand-before-grab ["hand004" 4 16]
   :cursors/hand-before-grab-gray ["hand004_gray" 4 16]
   :cursors/over-button ["hand002" 0 0]
   :cursors/sandclock ["sandclock" 16 16]
   :cursors/walking ["walking" 16 16]
   :cursors/no-skill-selected ["denied003" 0 0]
   :cursors/use-skill ["pointer004" 0 0]
   :cursors/skill-not-usable ["x007" 0 0]
   :cursors/bag ["bag001" 0 0]
   :cursors/move-window ["move002" 16 16]
   :cursors/princess ["exclamation001" 0 0]
   :cursors/princess-gray ["exclamation001_gray" 0 0]})

; TODO dispose all cursors , implement gdl.disposable
; => move to gdl ....

(defn ->context [ctx]
  {:context/cursors (mapvals (fn [[file x y]]
                               (->cursor ctx (str "cursors/" file ".png") x y))
                             cursors)})

(defmethod cdq.api.context/transact! :tx/cursor [[_ cursor-key] ctx]
  (cdq.api.context/set-cursor! ctx cursor-key)
  nil)
