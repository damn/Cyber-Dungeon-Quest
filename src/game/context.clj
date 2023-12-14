(ns game.context)

; during ingame only !
; because of id entity map ...

(defprotocol Context
  (get-entity [_ id])
  (entity-exists? [_ entity])
  (create-entity! [_ components-map])
  (destroy-to-be-removed-entities! [_]))

