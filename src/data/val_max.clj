(ns data.val-max)

; TODO assert hitpoints/mana positive integer ?
; specs ?
#_(defn- val-max-valid? [{val 0 max 1 :as val-max}]
  (and (vector? val-max)
       (= (count val-max) 2)
       (int? val) (>= val 0)
       (int? max) (>= max 0)
       (>= max val)))

(comment
 ; malli:
 (def my-schema
   [:and
    [:tuple pos-int? pos-int?]
    [:fn (fn [[vl mx]] (<= vl mx))]])
 ; TODO but pos-int? doesnt allow 0.
 )

(defn val-max
  ([v]
   [v v])
  ([v mx]
   [v mx]))

(defn val-max-ratio [[v mx]]
  (/ v mx))

(defn lower-than-max? [[v mx]]
  (< v mx))

(defn remainder-to-max [[v mx]]
  (- mx v))

(defn set-to-max [[_ mx]]
  [mx mx])

(defn- ->pos-int [value]
  (-> value int (max 0)))

(defn apply-val [f [v mx]]
  [(min (->pos-int (f v)) mx)
   mx])

(defn apply-max [f [v mx]]
  (let [mx (->pos-int (f mx))]
    [(min v mx)
     mx]))

(comment
 (apply-val (partial * 5) [3 5])
 [5 5]
 (apply-val (partial * -5) [3 5])
 [0 5]
 (apply-max (partial * -5) [3 5])
 [0 0]
 (apply-max (partial * 1.5) [3 5])
 [3 7]
 (apply-max (partial * 0.5) [3 5])
 [2 2]
 )

(defn apply-val-max-modifier [val-max [[val-or-max inc-or-mult] value]]
  (let [f (case inc-or-mult
            :inc  (partial + value)
            :mult (partial * value))]
    (case val-or-max
      :val (apply-val f val-max)
      :max (apply-max f val-max))))

(defn- inc<mult [[[val-or-max inc-or-mult] value]]
  (case inc-or-mult
    :inc 0
    :mult 1))

(defn apply-val-max-modifiers
  "First inc then mult"
  [val-max modifiers]
  (reduce apply-val-max-modifier
          val-max
          (sort-by
           inc<mult
           modifiers)))

(comment
 (apply-val-max-modifiers
  [5 10]
  {[:max :mult] 2
   [:val :mult] 1.5
   [:val :inc] 1
   [:max :inc] 1})
 ; -> [9 22]

 (apply-val-max-modifiers
  [9 22]
  {[:max :mult] 0.7
   [:val :mult] 1
   [:val :inc] -2
   [:max :inc] 0})
 ; -> [7 15]
 )

; TODO do not use 'value' outside of defeffect -> use proper minimal names at other functions
(defn affect-val-max-stat! [k {:keys [target value]}]
  (let [modifier value
        {val-old 0 :as val-max-old} (k @target)
        {val-new 0 :as val-max-new} (apply-val-max-modifier val-max-old modifier)]
    (swap! target assoc k val-max-new)
    (- val-new val-old)))
