(ns data.counter)

(defrecord ImmutableCounter [cnt maxcnt])

(defn create [maxcnt]
  {:pre [(>= maxcnt 0)]}
  (ImmutableCounter. 0 maxcnt))

(defn tick [counter delta]
  (update counter :cnt + delta))

(defn reset [counter]
  (assoc counter :cnt 0))

(defn stopped? [{:keys [cnt maxcnt]}]
  (>= cnt maxcnt))

(defn ratio [{:keys [cnt maxcnt] :as counter}]
  {:post [(<= 0 % 1)]}
  (if (or (zero? maxcnt) (stopped? counter))
    1
    (/ cnt maxcnt)))
