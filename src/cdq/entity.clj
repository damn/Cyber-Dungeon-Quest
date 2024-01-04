(ns cdq.entity
  (:require [x.x :refer [defsystem]]))

(defsystem create   [_])
(defsystem create!  [_ entity context])
(defsystem destroy! [_ entity context])
(defsystem tick     [_ entity* context])

(defsystem render-below   [_ entity* context])
(defsystem render-default [_ entity* context])
(defsystem render-above   [_ entity* context])
(defsystem render-info    [_ entity* context])
(defsystem render-debug   [_ entity* context])

(defrecord Entity [])

(defprotocol HasReferenceToItself
  (reference [_]))

(defprotocol State
  (state [_]))

(defprotocol Skills
  (has-skill? [_ skill]))

(defprotocol EffectModifiers
  (effect-source-modifiers [_ effect-type])
  (effect-target-modifiers [_ effect-type]))

(defprotocol Faction
  (enemy-faction [_])
  (friendly-faction [_]))
