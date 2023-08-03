(ns dev.start
  (:require [clojure.tools.namespace.repl :refer [disable-reload!]]))

(disable-reload!)

(comment
 (gdl.dev-loop/restart!)
 )
