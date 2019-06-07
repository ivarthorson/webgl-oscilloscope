(ns webgl-oscilloscope.core
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
            [thi.ng.geom.vector :as vec]))

(enable-console-print!)

(def gl-ctx (gl/gl-context "mainCanvas"))

(def state (atom {:server "http://localhost:3449"

                  :chans [{:name "source1" :signals ["sig1" "sig2" "sig3"]}
                          {:name "source2" :signals ["sig4" "sig5"]}]                          
                  :chans-selected [{:name "source1" :signals ["sig1"]}
                                   {:name "source2" :signals ["sig5"]}]

                  :time         0.0     ;; seconds
                  :time-history 200     ;; seconds
                  :time-lag     0.1     ;; seconds

                  :x-center     0.0
                  :x-scale      1.0
                  :y-center     0.0
                  :y-scale      1.0
                          
                  :grid        true
                  :grid-xticks  0.1
                  :grid-yticks  0.1
                  :axes        true
                  :autorange   true

                  ;;         view-rect (gl/get-viewport-rect gl)
                  :viewport-width  1000
                  :viewport-height 600
                          
                  :camera {:eye    (thi.ng.geom.vector/vec3 0.0 0.0 2.0)
                           :target (thi.ng.geom.vector/vec3 0.0 0.0 0.0)
                           :up     (thi.ng.geom.vector/vec3 0.0 1.0 0.0)
                           :fov    30
                           :aspect (/ 1000.0 600.0) ;; TODO: RESIZE THIS AS VIEWPORT CHANGES
                           :near  0.1   ;; Near clip
                           :far  1000   ;; far clip
                           }
                  }))

(def traces (atom []))

(def shader-spec {:uniforms {:view       :mat4
                             :proj       :mat4
                             :model      :mat4}
                  :attribs  {:position   :vec3}
                  :vs "void main() { gl_Position = proj * view * model * vec4(position, 1.0); }"
                  :fs "void main() { gl_FragColor = vec4(0, 0.0, 1.0, 1.0); }"
                  })

(def sine-wave (let [ts (range 0 3.14 0.1)
                     wave (map #(Math/sin %) ts)]
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


(def cog (-> sine-wave                       ;(poly/cog 0.5 20 [0.9 1 1 0.9])
             (gl/as-gl-buffer-spec {:normals false :fixed-color [1 0 0 1]})
             (gl/make-buffers-in-spec gl-ctx glc/static-draw)
             (assoc-in [:uniforms :proj] (gl/ortho (gl/get-viewport-rect gl-ctx)))
             (assoc :shader (shaders/make-shader-from-spec gl-ctx (shaders-basic/make-shader-spec-2d false)))
             ))

(defn draw-frame! [t]
  (swap! state assoc-in [:camera :eye] (thi.ng.geom.vector/vec3 1.0 0.0 t))
  
  ;; :target (thi.ng.geom.vector/vec3 1.0 0.0 0.0)

  (let [s @state]
    (doto gl-ctx
      (gl/clear-color-and-depth-buffer 0 0 0 1 1)
      (gl/draw-with-shader (-> (make-buffers triangle) ;;  shaded-triangle-buffer-thing    
                               ;; cam/apply updates the :view and :proj matrices of the hashmap
                               (cam/apply (cam/perspective-camera (:camera s)))
                               ;; And we can also update the model rotation matrix as well:
                               (assoc-in [:uniforms :model] (geom/rotate-y mat/M44 (* t 3.14)))))
      
      (gl/draw-with-shader (-> cog                               
                               (update-in [:attribs] dissoc :color)
                               (update-in [:uniforms] merge
                                          {:model (-> mat/M44 
                                                      ;; (geom/translate (vec/vec3 -0.48 0 0))
                                                      (geom/rotate-x t))
                                           :color [0 1 1 1]})))
      )))

;; To rotate or translate something, 
;;     (update-in something [:uniforms] merge {:model (geom/rotate-x mat/M44)})

(def running
  (anim/animate (fn [t] (draw-frame! t) true)))
