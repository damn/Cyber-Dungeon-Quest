(defproject cyberdungeonquest "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.clojens/clj-nstools "635f408a29"]
                 ;[nstools "0.2.5"]
                 [potemkin "0.4.6"]
                 [com.github.damn/grid2d "1.0"]
                 [com.github.damn/gdl "ae48536"]]
  :plugins [[jonase/eastwood "1.2.2"]
            [lein-ancient "1.0.0-RC3"]
            [lein-codox "0.10.8"]]
  :injections [(do (require 'x.intern) (x.intern/intern-nsx))]
  :target-path "target/%s/" ; https://stackoverflow.com/questions/44246924/clojure-tools-namespace-refresh-fails-with-no-namespace-foo
  :uberjar-name "cdq_3.jar"
  :omit-source true
  :jvm-opts ["-Xms256m"
             "-Xmx256m"
             "-Dvisualvm.display.name=CDQ"
             "-XX:-OmitStackTraceInFastThrow" ; disappeared stacktraces
             ; for visualvm profiling
             "-Dcom.sun.management.jmxremote=true"
             "-Dcom.sun.management.jmxremote.port=20000"
             "-Dcom.sun.management.jmxremote.ssl=false"
             "-Dcom.sun.management.jmxremote.authenticate=false"
             ]
  ; this from engine, what purpose?
  ;:javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]

  :global-vars {;*warn-on-reflection* true
                *print-level* 3}

  :aliases {"dev" ["run" "-m" "gdl.dev-loop" "game.start" "app"]})

; * Notes

; * openjdk@8 stops working with long error
; * fireplace 'cp' evaluation does not work with openJDK17
; * using openjdk@11 right now and it works.
; -> report to vim fireplace?

; :FireplaceConnect 7888
