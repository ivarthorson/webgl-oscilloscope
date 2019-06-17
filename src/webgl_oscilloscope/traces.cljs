(ns webgl-oscilloscope.traces
  (:require [thi.ng.geom.vector :as vec]
            [thi.ng.geom.gl.webgl.animator :as anim]
            [reagent.core :as r]
            [webgl-oscilloscope.color :as color]
            [webgl-oscilloscope.webgl :as webgl]
            [webgl-oscilloscope.widgets :as widgets]))

;; Traces are little collections of verticies that represent pieces of
;; a continuous line

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
  [traces t_expire]
  (vec (remove (partial expired? t_expire) traces)))

(defn latest-trace-named
  [traces name]
  (->> traces
       (filter (fn [h] (= name (:signal h))))
       (sort-by (fn [h] (last (:xs h))))
       (last)
       ((fn [h] 
          (last (:xs h))))))

(defn add-demo-trace-fragment
  [name t]
  (let [from (latest-trace-named traces name)
        to (+ 1.0 t)]
    (swap! traces conj (make-sine-trace from to))))

(comment
  (let [t (get-the-current-time)]
    (swap! add-demo-trace-fragment "Sine" (make-sine-trace))
    (swap! traces remove-expired-traces t))

)
