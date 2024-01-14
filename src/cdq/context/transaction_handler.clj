(ns cdq.context.transaction-handler
  (:require gdl.context
            [cdq.context :refer [transact! transact-all!]]))

(def log-txs? true) ; call 'record?' ??

(def txs-coll (atom {}))

(defn- debug-print-tx [tx]
  (pr-str (mapv #(cond
                  (instance? clojure.lang.Atom %) (str "<entity-atom{uid=" (:entity/uid @%) "}>")
                  (instance? gdl.backends.libgdx.context.image_drawer_creator.Image %) "<Image>"
                  (instance? gdl.graphics.animation.ImmutableAnimation %) "<Animation>"
                  (instance? gdl.context.Context %) "<Context>"
                  :else %)
                tx)))

(defn- add-tx-to-frame [txs-coll frame-num tx]
  (update txs-coll frame-num (fn [txs-at-frame]
                               (if txs-at-frame
                                 (conj txs-at-frame tx)
                                 [tx]))))

(comment
 (= (add-tx-to-frame (add-tx-to-frame {} 1 [:foo :bar1]) 1 [:foo :bar2])
    {1 [[:foo :bar1] [:foo :bar2]]})

 )

(def debug-print-txs? false)

(extend-type gdl.context.Context
  cdq.context/TransactionHandler
  (transact-all! [{:keys [context/game-logic-frame] :as ctx} txs]
    (doseq [tx txs :when tx]
      (try (let [result (transact! tx ctx)]
             (if (and (nil? result)
                      (not= :tx/cursor (first tx)))
               (do
                (when debug-print-txs?
                  (println @game-logic-frame "." (debug-print-tx tx)))
                (when log-txs?
                  (swap! txs-coll add-tx-to-frame @game-logic-frame tx)))
               (transact-all! ctx result)))
           (catch Throwable t
             (println "Error with transaction: \n" (debug-print-tx tx))
             (throw t))))))
