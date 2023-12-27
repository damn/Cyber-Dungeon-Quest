(ns mapgen.tiled-utils
  (:require [gdl.maps.tiled :as tiled])
  (:import com.badlogic.gdx.graphics.g2d.TextureRegion
           [com.badlogic.gdx.maps MapProperties MapLayers]
           [com.badlogic.gdx.maps.tiled TiledMap TiledMapTileLayer TiledMapTileLayer$Cell]
           com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile))

; "Tiles are usually shared by multiple cells."
; https://libgdx.com/wiki/graphics/2d/tile-maps#cells
; No copied-tile for AnimatedTiledMapTile yet (there was no copy constructor/method)
(def copy-tile
  (memoize
   (fn [^StaticTiledMapTile tile]
     (assert tile)
     (StaticTiledMapTile. tile))))

(defn ->static-tiled-map-tile [texture-region]
  (assert texture-region)
  (StaticTiledMapTile. ^TextureRegion texture-region))

(defn set-tile! [^TiledMapTileLayer layer [x y] tile]
  (let [cell (TiledMapTileLayer$Cell.)]
    (.setTile cell tile)
    (.setCell layer x y cell)))

(defn cell->tile [cell]
  (.getTile ^TiledMapTileLayer$Cell cell))

(defn add-layer! [tiled-map & {:keys [name visible properties]}]
  (let [layer (TiledMapTileLayer. (tiled/width  tiled-map)
                                  (tiled/height tiled-map)
                                  (tiled/get-property tiled-map :tilewidth)
                                  (tiled/get-property tiled-map :tileheight))]
    (.setName layer name)
    (when properties
      (.putAll ^MapProperties (tiled/properties layer) properties))
    (.setVisible layer visible)
    (.add ^MapLayers (tiled/layers tiled-map) layer)
    layer))

(defn ->empty-tiled-map []
  (TiledMap.))

(defn put! [^MapProperties properties key value]
  (.put properties key value))

(defn put-all! [^MapProperties properties other-properties]
  (.putAll properties other-properties))

(defn visible? [^TiledMapTileLayer layer]
  (.isVisible layer))
