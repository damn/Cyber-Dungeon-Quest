(ns data.val-max
  "val-max is a vector of 2 positive or zero integers  [value max-value]
  used for example as hitpoints [current max] or manapoints or damage
  [minimum maximum] in games
  there are 2 main functions:
  apply-val and apply-max
  which applies a function to value or max-value
  those functions make sure that val always remains smaller or equal than maximum
  for example damage [5 10] and we apply-val * 3
  will result in [10 10] damage
  where as apply-max / 3 => [3 3] damage.")

(defn val-max-ratio [[v mx]]
  (/ v mx))

(defn lower-than-max? [[v mx]]
  (< v mx))

(defn set-to-max [[_ mx]]
  [mx mx])

(defn- zero-or-pos-int [value]
  (-> value int (max 0)))

(defn apply-val [[v mx] f]
  (let [v (zero-or-pos-int (f v))]
    [(min v mx) mx]))

(defn apply-max [[v mx] f]
  (let [mx (zero-or-pos-int (f mx))]
    [(min v mx) mx]))

(comment
 (apply-val [3 5] (partial * 5))
 [5 5]
 (apply-val [3 5] (partial * -5))
 [0 5]
 (apply-max [3 5] (partial * -5))
 [0 0]
 (apply-max [3 5] (partial * 1.5))
 [3 7]
 (apply-max [3 5] (partial * 0.5))
 [2 2]
 )

; [operant operation]
(defn apply-val-max-modifier [val-max [[val-or-max inc-or-mult] value]]
  (let [f (case inc-or-mult
            :inc  (partial + value) ; TODO use operation op => :+ :- :*
            :mult (partial * value))]
    ((case val-or-max
       :val apply-val
       :max apply-max)
     val-max f)))

(defn- inc<mult [[[val-or-max inc-or-mult] value]]
  (case inc-or-mult
    :inc 0
    :mult 1))

(defn apply-val-max-modifiers
  "First inc then mult"
  [val-max modifiers]
  (reduce apply-val-max-modifier
          val-max
          (sort-by inc<mult modifiers)))

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
