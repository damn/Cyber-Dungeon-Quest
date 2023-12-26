(ns context.cursor
  (:require [gdl.context :refer [->cursor]]
            [utils.core :refer [safe-get mapvals]]
            cdq.context))

(extend-type gdl.context.Context
  cdq.context/Cursor
  (set-cursor! [{:keys [context/cursors] :as ctx} cursor-key]
    (gdl.context/set-cursor! ctx (safe-get cursors cursor-key))))

(def ^:private cursors
  {:cursors/default ["default" 0 0]
   :cursors/black-x ["black_x" 0 0]
   :cursors/denied ["denied" 16 16]
   :cursors/hand-grab ["hand003" 4 16]
   :cursors/hand-before-grab ["hand004" 4 16]
   :cursors/over-button ["hand002" 0 0]
   :cursors/sandclock ["sandclock" 16 16]
   :cursors/walking ["walking" 16 16]
   :cursors/no-skill-selected ["denied003" 0 0]
   :cursors/use-skill ["pointer004" 0 0]
   :cursors/skill-not-usable ["x007" 0 0]
   :cursors/bag ["bag001" 0 0]
   :cursors/move-window ["move002" 16 16]
   :cursors/princess ["exclamation001" 0 0]})

; TODO dispose cursors ( do @ gdl ? )
(defn ->context [ctx]
  {:context/cursors (mapvals (fn [[file x y]]
                               (->cursor ctx (str "cursors/" file ".png") x y))
                             cursors)})
