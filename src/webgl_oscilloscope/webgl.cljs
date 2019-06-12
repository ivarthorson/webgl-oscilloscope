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
            [thi.ng.geom.polygon :as poly]
            [thi.ng.geom.triangle :as tri]
            [thi.ng.geom.vector :as vec]
            [reagent.core :as r]
            [webgl-oscilloscope.color :as color]))

(def gl-ctx (gl/gl-context "mainCanvas"))

(def gl-state 
  (atom {:time         0.0             ;; seconds
         :time-history 200             ;; seconds
         :time-lag     0.1             ;; seconds

         :x-center     0.0
         :x-scale      1.0
         :y-center     0.0
         :y-scale      1.0
                          
         :grid        true
         :grid-xticks  0.1
         :grid-yticks  0.1

         ;;         view-rect (gl/get-viewport-rect gl)
         :viewport-width  1000
         :viewport-height 600
                          
         :camera {:eye    (thi.ng.geom.vector/vec3 0.0 0.0 2.0)
                  :target (thi.ng.geom.vector/vec3 0.0 0.0 0.0)
                  :up     (thi.ng.geom.vector/vec3 0.0 1.0 0.0)
                  :fov    30
                  :aspect (/ 1000.0 600.0) ;; TODO: RESIZE THIS AS VIEWPORT CHANGES
                  :near  0.1               ;; Near clip
                  :far  1000               ;; far clip
                  }}))

(def shader-spec {:uniforms {:view       :mat4
                             :proj       :mat4
                             :model      :mat4}
                  :attribs  {:position   :vec3}
                  :vs "void main() { gl_Position = proj * view * model * vec4(position, 1.0); }"
                  :fs "void main() { gl_FragColor = vec4(0, 0.0, 1.0, 1.0); }"
                  })
;; Add a shader to make it thicker
;; https://mattdesl.svbtle.com/drawing-lines-is-hard
(def thick-shader-spec {:uniforms {:view       :mat4
                                   :proj       :mat4
                                   :model      :mat4
                                   :normal     :vec2
                                   :miter      :float}
                        :attribs  {:position   :vec3}
                        :vs "void main() { 
                               vec2 p = position.xy + vec2(normal * thickness/2.0 * miter);
                               gl_Position = proj * view * model * vec4(p, 0.0, 1.0);"
                        :fs "void main() { gl_FragColor = vec4(0, 0.0, 1.0, 1.0); }"
                        })


;; TODO: test if only one vertex need be on screen for this to draw, or not.
(def x-axis (line/linestrip3 (map vector (range -100 100 10) (repeat 0.0) (repeat 0.0))))
(def y-axis (line/linestrip3 (map vector (repeat 0.0) (range -100 100 10) (repeat 0.0))))

(def grid (let [xmin -5.0                      ;; Grid min/max
                xmax  5.0                      ;; Grid min/max
                xg 0.1                         ;; Grid spacing
                ymin -5.0 
                ymax  5.0 
                yg 0.2]
            ;; TODO: This should be LINES type, but that doesn't fucking exist?!
            (line/linestrip3 
             (concat 
              (apply concat (for [x (range xmin xmax (* 2.0 xg))] ;; Vertical lines of grid
                              [[x ymin 0.0] [x ymax 0]
                               [[(+ xg x) ymax 0] (+ x xg) ymin 0.0]]))

              (apply concat (for [y (range ymin ymax (* 2.0 yg))] ;; Horizontal lines of grid
                              [[xmin y 0.0] [xmax y 0]
                               [xmax (+ yg y) 0] [xmin (+ yg y) 0.0 ]]))))))

(def sine-wave (let [ts (range 0 314.0 0.01)
                     wave (map #(Math/sin (* 5.0 %)) ts)]
                 (line/linestrip3 (map vector ts wave (repeat 0.0)))))

(def triangle (tri/triangle3 [[1 0 0] 
                              [0 0 0]
                              [0.5 1 0]]))

(def my-shader
  (shaders/make-shader-from-spec gl-ctx shader-spec))

(defn make-buffers
  "Ivar's default way to turn geometry into buffers (with proper shaders and camera) on it."
  [geom-obj]
  (-> geom-obj
      (geom/as-mesh {:mesh (glmesh/gl-mesh 3)})
      (gl/as-gl-buffer-spec {})
      (assoc :shader my-shader)
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)))


(def sine-obj (-> sine-wave
                 (gl/as-gl-buffer-spec {:normals false :fixed-color [1 0 0 1]})
                 (gl/make-buffers-in-spec gl-ctx glc/static-draw)
                 (assoc-in [:uniforms :proj] (gl/ortho (gl/get-viewport-rect gl-ctx)))
                 (assoc :shader (shaders/make-shader-from-spec gl-ctx (shaders-basic/make-shader-spec-2d false)))
                 ))

(def x-axis-obj (-> x-axis
                 (gl/as-gl-buffer-spec {:normals false :fixed-color [1 0 0 1]})
                 (gl/make-buffers-in-spec gl-ctx glc/static-draw)
                 (assoc-in [:uniforms :proj] (gl/ortho (gl/get-viewport-rect gl-ctx)))
                 (assoc :shader (shaders/make-shader-from-spec gl-ctx (shaders-basic/make-shader-spec-2d false)))
                 (update-in [:attribs] dissoc :color)
                 (update-in [:uniforms] merge {:color [1 1 1 1]})))

(def y-axis-obj (-> y-axis
                 (gl/as-gl-buffer-spec {:normals false :fixed-color [1 0 0 1]})
                 (gl/make-buffers-in-spec gl-ctx glc/static-draw)
                 (assoc-in [:uniforms :proj] (gl/ortho (gl/get-viewport-rect gl-ctx)))
                 (assoc :shader (shaders/make-shader-from-spec gl-ctx (shaders-basic/make-shader-spec-2d false)))
                 (update-in [:attribs] dissoc :color)
                 (update-in [:uniforms] merge {:color [1 1 1 1]})))

(def grid-obj (-> grid
                 (gl/as-gl-buffer-spec {:normals false :fixed-color [1 0 0 1]})
                 (gl/make-buffers-in-spec gl-ctx glc/static-draw)
                 (assoc-in [:uniforms :proj] (gl/ortho (gl/get-viewport-rect gl-ctx)))
                 (assoc :shader (shaders/make-shader-from-spec gl-ctx (shaders-basic/make-shader-spec-2d false)))
                 (update-in [:attribs] dissoc :color)))

(defn draw-frame! [rs t]
  (let [s @gl-state]
    (when (:run rs)
      (swap! gl-state assoc-in [:camera :eye]    (thi.ng.geom.vector/vec3 t 0.0 4.0))
      (swap! gl-state assoc-in [:camera :target] (thi.ng.geom.vector/vec3 t 0.0 0.0)))


    (gl/clear-color-and-depth-buffer gl-ctx 0 0 0 1 1)      
    (gl/draw-with-shader gl-ctx (-> (make-buffers triangle) ;;  shaded-triangle-buffer-thing    
                                    ;; cam/apply updates the :view and :proj matrices of the hashmap
                                    (cam/apply (cam/perspective-camera (:camera s)))
                                    ;; And we can also update the model rotation matrix as well:
                                    (assoc-in [:uniforms :model] (geom/rotate-y mat/M44 (* t 3.14)))))    

    (when (get-in rs [:show-grid])
      (gl/draw-with-shader gl-ctx grid-obj))
    
    (when (get-in rs [:show-axes])
      (gl/draw-with-shader gl-ctx x-axis-obj)
      (gl/draw-with-shader gl-ctx y-axis-obj))

    
    (when (get-in rs [:chans 0 :checked])
      (gl/draw-with-shader gl-ctx (-> sine-obj
                                      (cam/apply (cam/perspective-camera (:camera s)))
                                      (update-in [:attribs] dissoc :color)
                                      (update-in [:uniforms] merge
                                                 {:color (color/color-rgba 0)}))))))
