(ns cdq.entity
  (:require [x.x :refer [defsystem]]))

(defsystem create-component [_])
(defsystem create  [_ entity* ctx])
(defsystem destroy [_ entity* ctx])
(defsystem tick    [_ entity* ctx])

(defsystem render-below   [_ entity* ctx])
(defsystem render-default [_ entity* ctx])
(defsystem render-above   [_ entity* ctx])
(defsystem render-info    [_ entity* ctx])
(defsystem render-debug   [_ entity* ctx])

(defrecord Entity [])

(defprotocol State
  (state [_])
  (state-obj [_]))

(defprotocol Skills
  (has-skill? [_ skill]))

(defprotocol EffectModifiers
  (effect-source-modifiers [_ effect-type])
  (effect-target-modifiers [_ effect-type]))

(defprotocol Faction
  (enemy-faction [_])
  (friendly-faction [_]))

(defprotocol Inventory
  (can-pickup-item? [_ item]))
