(ns x.temp
  (:require [x.x :refer [defcomponent]]
            [gdl.lc :as lc])
  (:import com.badlogic.gdx.utils.Disposable))

;; TODO remove all this '!'!'

(defn set-var-root [v value]
  (alter-var-root v (constantly value)))

(defn dispose [^Disposable obj]
  (.dispose obj))

(def events->listeners {})

; TODO could add (defevent :foo) -> which adds it to events->listeners
; and @ add-listener check (@ compile time?) if the event exists....

(defn add-listener [event listener]
  (alter-var-root #'events->listeners update event
                  #(conj (or % []) listener))
  nil)

(defmacro on [event & exprs]
  `(add-listener ~event
                 (fn []
                   ; TODO !
                   (println "On" (quote ~event) " exprs:" (quote ~exprs))
                   ~@exprs)))

(defn fire-event! [event]
  (doseq [listener (get events->listeners event)]
    (listener)))

(defmacro on-create  [& exprs] `(on :app/create  ~@exprs))
(defmacro on-destroy [& exprs] `(on :app/destroy ~@exprs))

(defmacro defmanaged [symbol init]
  `(do
    (declare ~symbol)

    (let [var# (var ~symbol)]
      (on-create
       ;(println "on-create " var# " init: " '~init)
       (set-var-root var# ~init))

      (on-destroy
       (when (:dispose (meta var#))
         (dispose ~symbol))))))
