(ns cdq.entity
  (:require [x.x :refer [defsystem]]))

(defsystem create-component [_ ctx])
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

(defprotocol Faction
  (enemy-faction [_])
  (friendly-faction [_]))

(defprotocol Inventory
  (can-pickup-item? [_ item]))
