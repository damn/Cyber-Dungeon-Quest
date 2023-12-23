(ns context.entity.all
  "Requires all components not required anywhere" ; actually they all should not be required anywhere and use protocols & extend-type
  (:require context.entity.animation
            context.entity.clickable
            context.entity.delete-after-animation-stopped
            context.entity.delete-after-duration
            context.entity.hp
            context.entity.image
            context.entity.line-render
            context.entity.mana
            context.entity.mouseover
            context.entity.modifiers
            context.entity.position
            context.entity.shout
            context.entity.skills
            context.entity.state
            context.entity.string-effect))
