(defproject cyberdungeonquest "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.damn/grid2d "1.0"]
                 [com.github.damn/gdl "5638f23"]
                 [com.github.damn/x.x "195784a"]
                 [reduce-fsm "0.1.4"]
                 [metosin/malli "0.13.0"]
                 ;[lein-hiera "2.0.0"]
                 ]
  :plugins [[jonase/eastwood "1.2.2"]
            [lein-ancient "1.0.0-RC3"]
            [lein-codox "0.10.8"]
            [lein-hiera "2.0.0"]]
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

  :global-vars {*warn-on-reflection* true
                *print-level* 3}

  :aliases {"dev" ["run" "-m" "gdl.backends.libgdx.dev" "app.start" "-main"]})

; * Notes

; * openjdk@8 stops working with long error
; * fireplace 'cp' evaluation does not work with openJDK17
; * using openjdk@11 right now and it works.
; -> report to vim fireplace?

; :FireplaceConnect 7888

(comment
 ; https://github.com/greglook/clj-hiera/blob/main/src/hiera/main.clj
 (do
  (require '[hiera.main :as hiera])

  (hiera/graph
   {:sources #{"src"}
    :output "target/hiera"
    :layout :horizontal
    :cluster-depth 0
    :external false
    :ignore #{"data"
              "utils"
              "mapgen"}}))
 )
