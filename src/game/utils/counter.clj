(ns game.utils.counter
  (:require [utils.core :refer [assoc-in!]]))

; FIXME probably counter should be on 0 after stopped not exceed count...
; string effect after stopped counter was set to low value/0 and suddenly the text appeared below
; after moving up

; TODO add 'looping' true / false for extra delta per frame added to new count after reached max
; for some counters necessary

; if (> maxdelta maxcnt) it could happen that after update it is again 'stopped?' in next update
; could assert (<= maxcnt maxdelta), but dont want to depend on game.components.update here.

; TODO :as counter => :counter/create

(defrecord ImmutableCounter [cnt maxcnt stopped?])

; TODO create
(defn make-counter [maxcnt]
  (ImmutableCounter. 0 maxcnt false))

; TODO just tick (tick -  protocol ?)
(defn tick* [{:keys [cnt maxcnt stopped?] :as counter} delta]
  (let [newcnt (+ cnt delta)
        stopped? (>= newcnt maxcnt)]
    (assoc counter
           :cnt (if stopped? (- newcnt maxcnt) newcnt)
           :stopped? stopped?)))

; TODO no this swap here ...
(defn update-counter! [entity delta ks]
  (let [counter (tick* (get-in @entity ks) delta)]
    (assoc-in! entity ks counter)
    ; (update-in! entity ks tick delta)
    (:stopped? counter)))

(defn ratio [{:keys [cnt maxcnt]}]
  (if (zero? maxcnt)
    1
    (/ cnt maxcnt)))

(defn update-finally-merge [m counter-key delta merge-map]
  (let [m (update m counter-key tick* delta)]
    (if (:stopped? (counter-key m))
      (merge m merge-map)
      m)))

(defn reset [counter] ; TODO has to set stopped? to false ! check existing code
  (assoc counter
         :cnt 0
         :stopped? false))
