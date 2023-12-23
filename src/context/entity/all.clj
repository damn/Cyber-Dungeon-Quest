(ns context.entity.all
  "Requires all components not required anywhere" ; actually they all should not be required anywhere and use protocols & extend-type
  (:require (context.entity animation
                            clickable
                            delete-after-animation-stopped
                            delete-after-duration
                            faction
                            hp
                            image
                            line-render
                            mana
                            mouseover
                            modifiers
                            position
                            shout
                            skills
                            state
                            string-effect)))

; not here: body, inventory, movement
