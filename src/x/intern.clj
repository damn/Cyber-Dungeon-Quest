(ns x.intern)

; import-vars does not set ns-name to clojure.core when importing
(defn- throw-when-clojure-core-override [sym]
  (when-let [rslvd (ns-resolve 'clojure.core sym)]
    (when (= 'clojure.core (ns-name (:ns (meta rslvd))))
      (throw (IllegalArgumentException. (str "sym cannot be interned: " (name sym)))))))

(defn core* [nmspace]
  (def old-ns *ns*)

  (println "\n>>> Interning to clojure.core : " nmspace)
  (require nmspace)
  (in-ns 'clojure.core)
  (require 'potemkin.namespaces)

  (let [publics (keys (ns-publics (symbol nmspace)))]
    ;(println "(count publics)" (count publics))

    (doseq [sym publics]
      (throw-when-clojure-core-override sym))

    (let [syms (map #(symbol (str nmspace) (name %))
                    publics)]
      (println "import-vars: \n" syms)
      (eval
       `(potemkin.namespaces/import-vars ~@syms))))

  (in-ns (ns-name old-ns))
  (refer-clojure))

(defmacro core [nmspace]
  `(core* '~nmspace))

; TODO this could be a library to define custom project-specific namespace forms!
#_(def-ns-macro nsx
  (:like game.ns))

(defmacro nsx [nsnm & nsbody] ; change with ns-decl?+
  `(nstools.ns/ns+ ~nsnm
     (:like x.ns) ; also change @ game.ui.stage
     ~@nsbody))

;(println "(clojure.tools.namespace.parse/ns-decl? '(nsx foo))" (clojure.tools.namespace.parse/ns-decl? '(nsx foo)))

(defn- patch-clojure-tools-namespace []
  (require 'clojure.tools.namespace.parse) ; TODO required ?

  (defn ns-decl?+
    "Returns true if form is a (ns ...) declaration."
    [form]
    (and (list? form) (or (= 'ns (first form))
                          (= 'nsx (first form))))) ; change with defmacro nsx
  (intern 'clojure.tools.namespace.parse 'ns-decl?  ns-decl?+)

  (def ^:private ns-clause-head-names+
    "Set of symbol/keyword names which can appear as the head of a
    clause in the ns form."
    #{"use" "require" "require-macros" "like"})
  (intern 'clojure.tools.namespace.parse 'ns-clause-head-names  ns-clause-head-names+))

(defn intern-nsx []
  (patch-clojure-tools-namespace)
  ; TODO select which symbols .... do this sparingly , only 'ns+' / 'x' !
  (core nstools.ns)
  (core x.intern))

;; Alternative:

;[im.chit/vinyasa.inject "0.2.0"]:
;
;         :injections [(require 'vinyasa.inject)
;                      (vinyasa.inject/inject 'clojure.core
;                        '[[clojure.tools.namespace.repl refresh]
;                          [clojure.pprint pprint pp]])
