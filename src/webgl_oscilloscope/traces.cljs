(ns webgl-oscilloscope.traces
  (:require [thi.ng.geom.vector :as vec]
            [thi.ng.geom.gl.webgl.animator :as anim]
            [reagent.core :as r]
            [webgl-oscilloscope.color :as color]
            [webgl-oscilloscope.webgl :as webgl]
            [webgl-oscilloscope.widgets :as widgets]))

;; Traces are little collections of verticies that represent lines
;; Represented as hashmaps:


(def traces (atom []))

(defn make-sine-trace
  [from to & [sample-period]]
  (let [xs (vec (range from to sample-period)) ]
   {:source "Demo"
    :signal "Sine"
    :xs xs
    :ys (mapv #(- (Math/sin %) 0.2) xs)}))

(defn make-square-trace
  [from to & [sample-period]]
  (let [xs (vec (range from to sample-period)) ]
   {:source "Demo"
    :signal "Cosine"
    :xs xs
    :ys (mapv #(Math/cos %) xs)}))

(defn expired?
  "Returns true when the signal is older than t_expire"
  [t_expire trace]
  (< (last (:xs trace)) t_expire))

(defn remove-expired-traces
  [t_expire traces]
  (vec (remove (partial expired? t_expire) traces)))

;; (defn )

