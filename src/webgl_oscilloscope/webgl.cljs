(ns webgl-oscilloscope.webgl
  (:require [thi.ng.geom.core :as geom]
            [thi.ng.geom.gl.camera :as cam]
            [thi.ng.geom.gl.core :as gl]
            [thi.ng.geom.gl.glmesh :as glmesh]
            [thi.ng.geom.gl.shaders :as shaders]
            [thi.ng.geom.gl.shaders.basic :as shaders-basic]
            [thi.ng.geom.gl.webgl.animator :as anim]
            [thi.ng.geom.gl.webgl.constants :as glc]
            [thi.ng.geom.line :as line]
            [thi.ng.geom.matrix :as mat]
            [thi.ng.geom.vector :as vec]
            [reagent.core :as r]
            [webgl-oscilloscope.color :as color]))

(def gl-ctx (gl/gl-context "mainCanvas"))

(defn make-grid
  [grid-spacing]
  (let [xmin -5.0
        xmax  5.0
        xg (* 0.5 grid-spacing)
        ymin -5.0 
        ymax  5.0 
        yg grid-spacing]
    (vec 
     (concat      ;; TODO: This should be LINES type, but that doesn't exist in thi.ng/geom?
      (apply concat (for [x (range xmin xmax (* 2.0 xg))] ;; Vertical lines of grid
                      [[x ymin 0.0] [x ymax 0]
                       [[(+ xg x) ymax 0] (+ x xg) ymin 0.0]]))

      (apply concat (for [y (range ymin ymax (* 2.0 yg))] ;; Horizontal lines of grid
                      [[xmin y 0.0] [xmax y 0]
                       [xmax (+ yg y) 0] [xmin (+ yg y) 0.0 ]]))))))

(def gl-state 
  (atom {:grid   {:xyzs (make-grid 0.2)
                  :color [0.2 0.2 0.2 1]}
         :x-axis {:xyzs (mapv vector (range -100 100 10) (repeat 0.0) (repeat 0.0))
                  :color [1 1 1 1]}
         :y-axis {:xyzs (mapv vector (repeat 0.0) (range -100 100 10) (repeat 0.0))
                  :color [1 1 1 1]}

         :camera {:eye    (thi.ng.geom.vector/vec3 0.0 0.0 2.0)
                  :target (thi.ng.geom.vector/vec3 0.0 0.0 0.0)
                  :up     (thi.ng.geom.vector/vec3 0.0 1.0 0.0)
                  :fov    30
                  :aspect (/ 1000.0 600.0) ;; TODO: RESIZE THIS AS VIEWPORT CHANGES
                  :near  0.1               ;; Near clip
                  :far  1000               ;; far clip
                  }}))

(defn- make-linestrip-obj
  "Given a list of xyz vectors, turn that into an object that can be drawn with gl/draw-with-shader"
  [xyz-vecs rgba]
  (-> xyz-vecs
      (line/linestrip3)
      (gl/as-gl-buffer-spec {:normals false 
                             :fixed-color rgba})
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)
      (assoc-in [:uniforms :proj] (gl/ortho (gl/get-viewport-rect gl-ctx)))
      (assoc :shader (shaders/make-shader-from-spec gl-ctx (shaders-basic/make-shader-spec-2d false)))
      (update-in [:attribs] dissoc :color)
      (update-in [:uniforms] merge {:color rgba})))

(defn- to-webgl-linestrip
  "TODO: RENAME THIS FUNCTION TO BE MORE SPECIFIC. IT IS ONLY USED BY _TRACES_...not general linestrip objs
  rs = {source, chan, pos, scl}
  dat = [{chan, xyzs}
  gl-linestrip-obj = f(rs, dat)"
  [{:keys [position scale signal color] :as reagent-state}
   {:keys [xyzs] :as dat}]
  (let [scale (or scale 1.0)
        position (or position 0.0)
        color (or color [1 1 1 1])]
    (make-linestrip-obj (map (fn [[x y z]]
                               [x (+ position (* y scale)) z])                             
                             xyzs)
                        color)))

(defn get-linestrip-obj
  "In short, this is a manual memoization of what's under linestrip-obj.
  Gets the linestrip object from the hashmap sitting in STATE-ATOM at location PATH. 
  If a :linestrip-obj key already exists, the contents of it is used.
  If it doesn't exist, that a new linestrip object is created under :linestrip-obj, 
  and is built using the :xyzs and :color properties. The new linestrip object is returned.
  Optional argument COLOR lets you override the :color property."
  [state-atom path & [color]]
  (let [h (get-in @state-atom path {})]
    (if-let [linestrip-obj (:linestrip-obj h)]
      linestrip-obj
      (let [c (or color (:color h) [1 1 1 1])
            xyzs (:xyzs h)] 
        (if-not xyzs
          (println "WARNING: xyzs undefined for " path)
          (or (get h :linestrip-obj)              
              (let [new-linestrip-obj (make-linestrip-obj xyzs c)]
                (swap! state-atom assoc-in (concat path [:linestrip-obj]) new-linestrip-obj)
                new-linestrip-obj)))))))

(defn clear-linestrip-obj!
  "Deletes a cached linestrip-obj, and garbage collects it."
  []
  :TODO)

(defn draw-frame! [reagent-state trace-chunks t]
  (let [rs @reagent-state
        s @gl-state]
    (when (:run rs)
      (swap! gl-state assoc-in [:camera :eye]    (thi.ng.geom.vector/vec3 t 0.0 4.0))
      (swap! gl-state assoc-in [:camera :target] (thi.ng.geom.vector/vec3 t 0.0 0.0)))

    (gl/clear-color-and-depth-buffer gl-ctx 0 0 0 1 1)      

    (when (get-in rs [:show-grid])
      (gl/draw-with-shader gl-ctx (get-linestrip-obj gl-state [:grid])))
    
    (when (get-in rs [:show-axes])
      (gl/draw-with-shader gl-ctx (get-linestrip-obj gl-state [:x-axis]))
      (gl/draw-with-shader gl-ctx (get-linestrip-obj gl-state [:y-axis])))

    ;; Loop for all selected signals and for all trace chunks of those signals
    (let [should-display? (into #{} (map :signal (filter :checked (:chans rs))))
          chunks @trace-chunks]
      (dotimes [j (count chunks)]
        (let [trace (get-in chunks [j])
              trace-name (:signal trace)]
          (when (should-display? trace-name)
            (let [reagent-signal-hash (first (filter #(= trace-name (:signal %)) (:chans rs))) ;; TODO: Remove this searching process!
                  ;; TODO: use (get-linestrip-obj) here instead, and pass the reagent-signal-hash color. 
                  linestrip-obj (or (get-in chunks [j :linestrip-obj])
                                    (let [new-linestrip-obj (to-webgl-linestrip reagent-signal-hash trace)]
                                      (swap! trace-chunks assoc-in [j :linestrip-obj] new-linestrip-obj)
                                      new-linestrip-obj))]
              (gl/draw-with-shader gl-ctx (-> linestrip-obj
                                              (cam/apply (cam/perspective-camera (:camera s))))))))))))
