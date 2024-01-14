(ns cdq.context.transaction-handler
  (:require gdl.context
            [cdq.context :refer [transact! transact-all!]]))

(def record-txs? true)

(def frame->txs (atom {}))

(defn- add-tx-to-frame [frame->txs frame-num tx]
  (update frame->txs frame-num (fn [txs-at-frame]
                                 (if txs-at-frame
                                   (conj txs-at-frame tx)
                                   [tx]))))

(comment
 (= (-> {}
        (add-tx-to-frame 1 [:foo1 :bar1])
        (add-tx-to-frame 1 [:foo2 :bar2]))
    {1 [[:foo1 :bar1] [:foo2 :bar2]]})
 )

(def debug-print-txs? false)

(defn- debug-print-tx [tx]
  (pr-str (mapv #(cond
                  (instance? clojure.lang.Atom %) (str "<entity-atom{uid=" (:entity/uid @%) "}>")
                  (instance? gdl.backends.libgdx.context.image_drawer_creator.Image %) "<Image>"
                  (instance? gdl.graphics.animation.ImmutableAnimation %) "<Animation>"
                  (instance? gdl.context.Context %) "<Context>"
                  :else %)
                tx)))

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
                (when record-txs?
                  (swap! frame->txs add-tx-to-frame @game-logic-frame tx)))
               (transact-all! ctx result)))
           (catch Throwable t
             (println "Error with transaction: \n" (debug-print-tx tx))
             (throw t)))))

  (frame->txs [_ frame-number]
    (@frame->txs frame-number)))
