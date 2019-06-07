(ns webgl-oscilloscope.core
  (:require [thi.ng.geom.gl.core :as gl]
            [thi.ng.geom.matrix :as mat]
            [thi.ng.geom.core :as geom]
            [thi.ng.geom.line :as line]
            [thi.ng.geom.triangle :as tri]
            [thi.ng.geom.gl.glmesh :as glmesh]
            [thi.ng.geom.gl.glmesh :as glmesh]
            [thi.ng.geom.gl.shaders :as shaders]
            [thi.ng.geom.gl.webgl.constants :as glc]
            [thi.ng.geom.gl.camera :as cam]
            [thi.ng.geom.gl.webgl.animator :as anim]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello world!"}))

(defn on-js-reload []
  ;; optionally touch your app-state (to force rerendering if needed)
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

;; Get the GL context
(def gl-ctx (gl/gl-context "mainCanvas"))

;; Create a perspective camera
(def camera (cam/perspective-camera {:far 100
                                     :fov 30}))

(def shader-spec
  {;; Defines uniform 4x4 matrices that will be set before rendering starts
   :uniforms {:view       :mat4
              :proj       :mat4
              :model      :mat4}
   :attribs  {:position   :vec3}
   :vs "void main() {
          gl_Position = proj * view * model * vec4(position, 1.0);
       }"
   :fs "void main() {
           gl_FragColor = vec4(0, 0.0, 1.0, 1.0);
       }"
   })


(def sine-wave 
  (let [ts (range 0 3.14 0.01)
        wave (map #(Math/sin %) t)]
    (line/linestrip2 (map vector t wave))))

(def triangle 
  (geom/as-mesh (tri/triangle3 [[1 0 0] [0 0 0] [0 1 0]])
                {:mesh (glmesh/gl-mesh 3)}))

(defn spin
  [t]
  (geom/rotate-y mat/M44 (/ t 1.0)))

(defn combine-model-shader-and-camera
  [model shader-spec camera]
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (shaders/make-shader-from-spec gl-ctx shader-spec))
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)
      (cam/apply camera)))

(defn draw-frame! [t]
  (doto gl-ctx
    (gl/clear-color-and-depth-buffer 0 0 0 1 1)
    (gl/draw-with-shader (assoc-in (combine-model-shader-and-camera triangle
                                                                    shader-spec 
                                                                    camera)
                                   [:uniforms :model] (spin t)))))

(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))

;; (println camera)


;; (js/alert "Am I connected?")

