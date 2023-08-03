; MOVE TO screens/ingame
; => TODO move to gdl ??? (but GUI input still available ... )
(ns game.running)

; TODO after death & restart -> still running!
; session-state reset !
; (when not pausing/resuming gamestyle)

(def running (atom true))
