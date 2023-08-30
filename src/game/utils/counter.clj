(ns game.utils.counter
  (:require [utils.core :refer [assoc-in!]]))

; FIXME probably counter should be on 0 after stopped not exceed count...
; string effect after stopped counter was set to low value/0 and suddenly the text appeared below
; after moving up

; TODO add 'looping' true / false for extra delta per frame added to new count after reached max
; for some counters necessary

; if (> maxdelta maxcnt) it could happen that after update it is again 'stopped?' in next update
; could assert (<= maxcnt maxdelta), but dont want to depend on game.components.update here.

; TODO stopped? or is-stopped or is-stopped? check clojure coding guidelines
(defrecord ImmutableCounter [cnt maxcnt stopped?])

; TODO protocol tick/ratio/reset ? is-stopped?

(defn make-counter [maxcnt] ; TODO :as counter => :counter/create
  (ImmutableCounter. 0 maxcnt false))

; TICK of COUNTER !
; => counter is a component which system should run through

(defn tick* [{:keys [cnt maxcnt stopped?] :as counter} delta]
  (let [newcnt (+ cnt delta)
        stopped? (>= newcnt maxcnt)]
    (assoc counter
           :cnt (if stopped? (- newcnt maxcnt) newcnt)
           :stopped? stopped?)))

; TODO do-tick-and-stopped?
; TODO counters dont loop by default -> sometimes overflow error
; set :loop false
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

;; TODO
; :counter (make-counter 200)
; => make counter into a component!
; just :counter 200 in an entity => creatres it !

; counter has also extend

; TODO leave nested counters as they are for now !
; can do later ! also performance !
#_(defcomponent :counter ; shout
  (db/create [_ duration]
    (make-counter duration))
  (tick [_ v delta]
    (tick* v delta)))

; TODO WAIT !!! I moved tick into the tick system
; but  am I still manually updating counters ?
; have to see where I changed it !!
; diff search make-counter !
; no more updating counters ! (anyway not when saving delta-time-since-start-without-paused.

; TODO EVERYWHERE COUNTER !
; => sub-components should also get updated
; => datomic

; => If I know the keys of a component I can always make them into a record ...
; cell/counter/etc.
; => but then those records could even implement protocols
; shit ! ?

(comment
 (defrecord Counter [a b c]
  Tick
  (tick [_ ])
  RenderAbove
  (render-above [])
  )

 ; NO  ! Better :
 (defrecord Counter [a b c])

 (defcomponent :counter
   (db/create [_ v] (->Counter v))
   )
 ; but then we can also combine keywords with defrecords
 ; => then we could also combine them with protocols ?
 ; but only for those components who are maps themself
 ; the link is done @ create-system, it doesnt have to be more complicated than that!
 )

; but that only works for components which are hashmaps also
; but I could wrap it and have the 'value' ?

; :position #componentRecord [ value = [x y] ]
; but then everuthing in extra records all the time , stupid....
; anyway I dont dispatch on the data but the key !
; TODO where tick?
; [x.systems.tick :refer :all]  ?

;; TODO !! IMPORTANT !!
; I made :counter into a component so everywhere where we removed make-counter
; check if tick is then called (two times ticking ) ?!

; update-finally-merge
; => a 'counter' can have different types of counters
; which calls a 'on-stop?'
; function (system)
; => counters are systems and components

; eveywhere we have a 'on-stop' function can be maybe simplified

; delete after duration is itself a counter component ?
; they are not ticking because not 'counter'
; so I have to make a hierarchy ???
; is-a counter ?
; but I have different logic, I also assoc :destroyed (entity-updates bring back ??)
