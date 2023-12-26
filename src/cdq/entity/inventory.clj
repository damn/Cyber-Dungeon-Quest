(ns cdq.entity.inventory)

(defprotocol InventoryCell
  (valid-slot? [_ item])
  )

(defprotocol Inventory
  (two-handed-weapon-and-shield-together? [_ cell item])
  )
; ILookup (get inventory cell)  == item
; not get-in ?

(defprotocol Item
  (stackable? [_ other-item])
  )
