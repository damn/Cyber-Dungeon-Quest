(ns utils.core
  (:require (clojure [pprint :refer (pprint)]
                     set)))

(defn- indexed ; from clojure.contrib.seq-utils (discontinued in 1.3)
  "Returns a lazy sequence of [index, item] pairs, where items come
 from 's' and indexes count up from zero.

 (indexed '(a b c d)) => ([0 a] [1 b] [2 c] [3 d])"
  [s]
  (map vector (iterate inc 0) s))

(defn positions ; from clojure.contrib.seq-utils (discontinued in 1.3)
  "Returns a lazy sequence containing the positions at which pred
	 is true for items in coll."
  [pred coll]
  (for [[idx elt] (indexed coll) :when (pred elt)] idx))

(defn find-first ; from clojure.contrib.seq-utils (discontinued in 1.3)
  "Returns the first item of coll for which (pred item) returns logical true.
  Consumes sequences up to the first match, will consume the entire sequence
  and return nil if no match is found."
  [pred coll]
  (first (filter pred coll)))

(defmacro def- ; from clojure.contrib.def (discontinued in 1.3)
  "Same as def but yields a private definition"
  [name value]
  (list 'def (with-meta name (assoc (meta name) :private true)) value))

(defn runmap [& more] (dorun (apply map more)))

(defmacro tm [expr]
  `(do
    (println "~" ~(str expr))
    (time ~expr)))

(defmacro dbg [expr]
  `(do
    (println "DEBUGGING: " ~(str expr) " => " ~expr)
    ~expr))

(defn- time-of [expr n]
  `(do
     (println "Time of: " ~(str expr))
     (time (dotimes [_# ~n] ~expr))))

(defmacro compare-times [n & exprs]
  (let [forms (for [expr exprs]
                (time-of expr n))]
    `(do ~@forms nil)))

(defmacro deflazygetter [fn-name & exprs]
  `(let [delay# (delay ~@exprs)]
     (defn ~fn-name [] (force delay#))))

(defn distinct-seq?
  "same as (apply distinct? coll) but returns true if coll is empty/nil."
  [coll]
  (if (seq coll)
    (apply distinct? coll)
    true))

(defn safe-merge
  "same as merge but asserts no key is overridden."
  [& maps]
  (let [ks (mapcat keys maps)]
    (assert (distinct-seq? ks) (str "not distinct keys: " (apply str (interpose "," ks)))))
  (apply merge maps))

(defn keywords-to-hash-map [keywords] ; other name
  (into {} (for [k keywords]
             [k (symbol (name k))])))

(defn assoc-in!  [a & args] (apply swap! a assoc-in  args))
(defn update-in! [a & args] (apply swap! a update-in args))

(defmacro ->! [a & forms]
  `(swap! ~a #(-> % ~@forms)))

(defn mapvals [f m]; in core !
  (into {} (for [[k v] m]
             [k (f v)])))

(defn pexp [form]
  (use 'clojure.pprint)
  (binding [*print-level* nil
            *print-meta* true]
    (pprint
     (macroexpand-1 form))))

; TODO :injections leiningen ! also pprint
; debugging injections also
(def pexpand-1 (comp pprint macroexpand-1))
(def pexpand   (comp pprint macroexpand))

(defn genmap
  "function is applied for every key to get value. use memoize instead?"
  [ks f]
  (zipmap ks (map f ks)))

(defn diagonal-direction? [[x y]]
  (and (not (zero? x))
       (not (zero? y))))

(defn int-posi [p] (mapv int p))

(defn log [& more]
  (println "~~~" (apply str more)))

;; Order

(defn define-order [order-k-vector]
  (apply hash-map
         (interleave order-k-vector (range))))

(defn sort-by-order [coll get-item-order-k order]
  (sort-by #((get-item-order-k %) order) < coll))

(defn order-contains? [order k]
  ((apply hash-set (keys order)) k))

#_(deftest test-order
  (is
    (= (define-order [:a :b :c]) {:a 0 :b 1 :c 2}))
  (is
    (order-contains? (define-order [:a :b :c]) :a))
  (is
    (not
      (order-contains? (define-order [:a :b :c]) 2)))
  (is
    (=
      (sort-by-order [:c :b :a :b] identity (define-order [:a :b :c]))
      '(:a :b :b :c)))
  (is
    (=
      (sort-by-order [:b :c :null :null :a] identity (define-order [:c :b :a :null]))
      '(:c :b :a :null :null))))



;;

(defn print-n-return [data] (println data) data)

(defn get-next-idx
  "returns the next index of a vector.
  if there is no next -> returns 0"
  [current-idx coll]
  (let [next-idx (inc @current-idx)]
    (reset! current-idx (if (get coll next-idx) next-idx 0))))

; (MathUtils/isEqual 1 (length v))
(defn approx-numbers [a b epsilon]
  (<=
    (Math/abs (float (- a b)))
    epsilon))

(defn round-n-decimals [x n]
  (let [z (Math/pow 10 n)]
    (float
      (/
        (Math/round (float (* x z)))
        z))))

(defn readable-number [x]
  {:pre [(number? x)]} ; do not assert (>= x 0) beacuse when using floats x may become -0.000...000something
  (if (or
        (> x 5)
        (approx-numbers x (int x) 0.001)) ; for "2.0" show "2" -> simpler
    (int x)
    (round-n-decimals x 2)))

(defn translate-to-tile-middle
  "translate position to middle of tile becuz. body position is also @ middle of tile."
  [p]
  (mapv (partial + 0.5) p))

;;

; TODO definitely move to engine

; TODO also for counters ? also ratio there
; but reset function ... well just its a separate function

(defn- val-max-valid? [{val 0 max 1 :as val-max}] ; TODO unused !!
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

; TODO assert hitpoints/mana positive integer ?
; => @ val-max ... => specs ?
(defn val-max
  ([val]     [val val])
  ([val max] [val max]))

(defn val-max-ratio [[val max]]
  (/ val max))

(defn lower-than-max? [[val max]]
  (< val max))

(defn remainder-to-max [[val max]] ; TODO maybe not necessary here, just do @ use case
  (- max val))

(defn set-to-max [[_ max]] ; only used @ player revive, not sure if necessary
  [max max])

(defn- apply* [f value] ; TODO naming!
  (-> value f int (max 0)))

(defn apply-val [f [val max]]
  [(min (apply* f val) max)
   max])

(defn apply-max [f [val max]] ; TODO value & max-value, or 'v' and 'mv'
  (let [max (apply* f max)]
    [(min val max)
     max]))

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

(defn- inc<mult [[[val-or-max mult-or-inc] value]]
  (case mult-or-inc
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

(defmacro when-seq [[aseq bind] & body]
  `(let [~aseq ~bind]
     (when (seq ~aseq)
       ~@body)))

(defmacro xor [a b]
  `(or (and (not ~a) ~b)
       (and ~a (not ~b))))

(defn assoc-ks [m ks v]
  (if (empty? ks)
    m
    (apply assoc m (interleave ks (repeat v)))))

(let [obj (Object.)]
  (defn safe-get [m k]
    (let [result (clojure.core/get m k obj)]
      (if (= result obj)
        (throw (IllegalArgumentException. (str "Cannot find " k)))
        result))))
