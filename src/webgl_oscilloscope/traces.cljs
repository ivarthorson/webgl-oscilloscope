(ns webgl-oscilloscope.traces
  (:require [thi.ng.geom.vector :as vec]
            [thi.ng.geom.gl.webgl.animator :as anim]
            [reagent.core :as r]
            [webgl-oscilloscope.color :as color]
            [webgl-oscilloscope.webgl :as webgl]
            [webgl-oscilloscope.widgets :as widgets]))

;; Traces are little collections of verticies that represent pieces of
;; a continuous line

(defn make-sine-trace
  [from to & [sample-period]]
  (let [sample-period (or sample-period 0.02)
        xs (vec (range from to sample-period)) 
        ys (map #(Math/sin (* 5.0 %)) xs)
        zs (repeat 0.0)
        xyzs (mapv vector xs ys zs)]
   {:source "Demo"
    :signal "Sine"
    :xyzs xyzs}))

(defn make-cosine-trace
  [from to & [sample-period]]
  (let [sample-period (or sample-period 0.02)
        xs (vec (range from to sample-period)) 
        ys (map #(Math/cos (* 5.0 %)) xs)
        zs (repeat 0.0)
        xyzs (mapv vector xs ys zs)]
   {:source "Demo"
    :signal "Cosine"
    :xs xs
    :xyzs xyzs}))

(def trace-chunks (atom []))

(defn- latest-time
  "Returns the latest time of a trace chunk."
  [trace]
  (first (last (:xyzs trace))))

(defn- expired?
  "Returns true when the signal is older than t_expire"
  [t_expire trace]
  (< (latest-time trace) t_expire))

(defn- latest-time-of
  "Returns the latest time of any chunk that matches name."
  [traces name]
  (->> traces
       (filter (fn [h] (= name (:signal h))))
       (sort-by latest-time)
       (last)
       (latest-time)))

;; -----------------------------------------------------------------------------
;; PUBLIC THINGS

(defn list-channels
  []
  (mapv (fn [trace]
          {:source (:source trace) 
           :signal (:signal trace)}) @trace-chunks))

(defn delete-linestrips
  "The linestrips are stored as sub-keys of trace-chunks.
  NOTE: linestrips are essentially cached and need to be invalidated when
  you alter relevant display state in reagent state. "
  [chunks]
  (mapv (fn [h] (dissoc h :linestrip-obj)) chunks))

(defn remove-expired-traces!
  [t_expire]
  (swap! trace-chunks
         (fn [chunks]
           (vec (remove (partial expired? t_expire) chunks)))))

(defn remove-all-traces!
  "Removes all traces and resets the history."
  []
  (reset! trace-chunks []))

(defn add-demo-sine-fragment
  "Adds sine trace chunks as needed."
  [t]
  (let [from (or (latest-time-of @trace-chunks "Sine") t)
        to (+ 3.0 t)]
    (when (and from to)
      (swap! trace-chunks conj (make-sine-trace from to)))))

(defn add-demo-cosine-fragment
  [t]
  (let [from (or (latest-time-of @trace-chunks "Cosine") t)
        to (+ 3.0 t)]
    (when (and from to)
      (swap! trace-chunks conj (make-cosine-trace from to)))))
