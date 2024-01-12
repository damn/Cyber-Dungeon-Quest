(ns cdq.entity.all
  "Requires all components not required anywhere" ; actually they all should not be required anywhere and use protocols & extend-type
  (:require (cdq.entity animation
                        clickable
                        delete-after-animation-stopped
                        delete-after-duration
                        faction
                        hp
                        image
                        line-render
                        mana
                        mouseover
                        position
                        projectile-collision
                        shout
                        skills
                        state
                        state
                        string-effect)))

; not here: body, inventory, movement
