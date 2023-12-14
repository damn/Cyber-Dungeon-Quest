(ns game.context)

; during ingame only context, maybe name it like this ?
; because of id entity map ...
; play-sound! could be in gdl

(defprotocol Context
  (get-entity [_ id])
  (entity-exists? [_ entity])
  (create-entity! [_ components-map]
                  "Entities should not have :id component, will get added.
                  Calls entity/create system on the components-map
                  Then puts it into an atom and calls entity/create! system on all components.")
  (destroy-to-be-removed-entities! [_]
                                   "Calls entity/destroy and entity/destroy! on all entities which are marked as ':destroyed?'")
  (play-sound! [_ file]
               "Sound is already loaded from file, this will perform only a lookup for the sound and play it."))

