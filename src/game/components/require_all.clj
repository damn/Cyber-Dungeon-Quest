(ns
  "Requires all components not required anywhere"
  game.components.require-all
  (:require game.components.body.rotation-angle
            game.components.animation
            game.components.delete-after-animation-stopped?
            game.components.delete-after-duration
            ;game.components.glittering
            game.components.image
            game.components.line-render
            game.components.mana
            game.components.sleeping))
