(ns
    geogrid2svg
  "A geographic grid protocol and associated function"
  (:use geoprim
        geogrid)
  (:require [quickthing]
            [clojure.java.io      :as io]
            [thi.ng.geom
             [core                :as geom]
             [matrix              :as matrix]]
            [thi.ng.geom.viz.core :as viz]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.math.core     :as math]
            [thi.ng.ndarray.core  :as ndarray]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn
  data-to-heatmap-matrix
  "`thing/geom` uses its own matrix type to draw heatmaps.
  Convert our data vector to this matrix type"
  [given-grid
   max-val]
  (->> (normalized-data given-grid
                        max-val)
       (#(ndarray/ndarray
           :float64
           %
           (reverse
             (dimension-pix
               given-grid))))))

(defn
  to-heatmap
  "blah blah"
  [given-grid
   & [{:keys [^double
              max-val
              ^double
              top
              ^double
              bottom
              ^double
              right
              ^double
              left]
       :or   {max-val nil
              top    0.0
              bottom 0.0
              right  0.0
              left   0.0}
       :as   overruns}]]
  (let[[^long
        width-pix
        ^long
        height-pix] (dimension-pix
                      given-grid)
       [^double
        eas-res
        ^double
        sou-res]    (eassou-res
                   given-grid)]
    (->
      {:x-axis (viz/linear-axis
                 {:domain  [left
                            (- ^long width-pix
                               ^double right)]
                  :range   [0
                            (*
                              eas-res
                              (-
                                width-pix
                                left
                                right))]
                  :visible false})
       :y-axis (viz/linear-axis
                 {:domain  [top
                            (-
                              height-pix
                              bottom)]
                  :range   [0
                            (*
                              sou-res
                              (-
                                height-pix
                                top
                                bottom))]
                  :visible false})
       :data   [{:matrix        (data-to-heatmap-matrix given-grid
                                                        max-val)
                 :value-domain  [-1
                                 1] ;; max-value is 1.0
                 :palette       quickthing/red-blue-colors
                 :palette-scale viz/linear-scale
                 :layout        viz/svg-heatmap}]}
      viz/svg-plot2d-cartesian)))
;;.[EXAMPLE]
#_
;; req: `geogrid4image` `geogrid4seq` `svgmaps`
(let[;;
     rain-grid-original
     (geogrid4image/read-file
       "../geogrid4image/rain-2011-03.tif"
       (geoprim/point-eassou
         0
         0)
       0.1
       0.1)
     ;;
     rain-grid
     #_
     rain-grid-original
     (geogrid4seq/convert-to
       rain-grid-original)
     ;;
     region
     (region ;; taiwan region
       (point
         25.76
         119.74)
       (point
         21.74
         122.26))
     ;;
     overruns
     (:overruns
      (adjusted-crop-region-to-grid
        region
        rain-grid))
     ;;
     subgrid
     (geogrid/subregion
       rain-grid
       region)]  
  (spit
    "test.svg"
    (svg/serialize
      (svgmaps/to-standard-svg
        (to-heatmap
          (->
            subgrid
            geogrid4seq/convert-to)
          overruns)
        (dimension
          region)))))
