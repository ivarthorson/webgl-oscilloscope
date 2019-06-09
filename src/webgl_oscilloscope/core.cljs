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
            [thi.ng.geom.vector :as vec]
            [reagent.core :as r]
            [webgl-oscilloscope.widgets :as widgets]))

(enable-console-print!)

(def reagent-state (r/atom {:server "http://localhost:3449"
                            
                            :chans [{:id 0 :source "Demo" :signal "Sine" :checked true}
                                    {:id 1 :source "Demo" :signal "sig2" :checked true}
                                    {:id 2 :source "Demo" :signal "sig3"}
                                    {:id 3 :source "source2" :signal "sig5"}]
                            
                            :x-axis true

                            :run true
                            }))

(def gl-state (atom {:time         0.0 ;; seconds
                     :time-history 200 ;; seconds
                     :time-lag     0.1 ;; seconds

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

(def gl-ctx (gl/gl-context "mainCanvas"))

(def traces (atom []))

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

(def sine-wave (let [ts (range 0 31.4 0.01)
                     wave (map #(Math/sin (* 5.0 %)) ts)]
                 (line/linestrip3 (map vector ts wave (repeat 0.0)))))

;; TODO: test if only one vertex need be on screen for this to draw, or not.
(def x-axis (let [ts (range -100 100 10)]
              (line/linestrip3 (map vector ts (repeat 0.0) (repeat 0.0)))))

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
                 ))

(defn draw-frame! [t]
  (let [s @gl-state
        rs @reagent-state]
    (when (:run rs)
      (swap! gl-state assoc-in [:camera :eye]    (thi.ng.geom.vector/vec3 t 0.0 4.0))
      (swap! gl-state assoc-in [:camera :target] (thi.ng.geom.vector/vec3 t 0.0 0.0)))

    (gl/clear-color-and-depth-buffer gl-ctx 0 0 0 1 1)      
    (gl/draw-with-shader gl-ctx (-> (make-buffers triangle) ;;  shaded-triangle-buffer-thing    
                                    ;; cam/apply updates the :view and :proj matrices of the hashmap
                                    (cam/apply (cam/perspective-camera (:camera s)))
                                    ;; And we can also update the model rotation matrix as well:
                                    (assoc-in [:uniforms :model] (geom/rotate-y mat/M44 (* t 3.14)))))    
    
    (when (get-in rs [:x-axis])
      (gl/draw-with-shader gl-ctx (-> x-axis-obj            
                                      (cam/apply (cam/perspective-camera (:camera s)))
                                      (update-in [:attribs] dissoc :color)
                                      )))

    (when (get-in rs [:chans 0 :checked])
      (gl/draw-with-shader gl-ctx (-> sine-obj
                                      (cam/apply (cam/perspective-camera (:camera s)))
                                      (update-in [:attribs] dissoc :color)
                                      #_ (update-in [:uniforms] merge
                                                    {:model (-> mat/M44 
                                                                ;; (geom/translate (vec/vec3 -0.48 0 0))
                                                                (geom/rotate-x (* 3.14 t)))
                                                     :color [0 0.5 1 1]}))))
    ))



;; To rotate or translate something, 
;;     (update-in something [:uniforms] merge {:model (geom/rotate-x mat/M44)})

(defn checkboxes
  []
  (let [s @reagent-state]
    [:table
     [:tbody
      (for [{:keys [id source signal checked] :as c} (:chans s)]
        ^{:key (str "checkbox-chan-" id) }
        [:tr [:td [:label [:input {:type "checkbox"
                                   :checked checked
                                   :on-change #(swap! reagent-state assoc-in [:chans id :checked] (not checked))}]
                   [:font {:class (str "trace0" id)} (str source " / " signal)]]]])]]))

(defn top-bar
  [user-input]
  [:table
     [:tbody
      [:tr                    
       [:td {:width "30px"}]
       [:td [:button {:on-click #(print "TODO: Clear Everything")}
             "Clear"]]
       [:td [:button {:on-click #(swap! reagent-state update-in [:x-axis] (fn [x] (not x)))}
             "Axes"]]
       [:td [:button {:on-click #(swap! reagent-state update-in [:grid] (fn [x] (not x)))}
             "Grid"]]
       [:td [:button {:on-click #(print "TODO: Reset Y axis zoom")}
             "Auto Range"]]
       [:td [:select {:class "userInput"
                      :default-value "1"
                      :on-change #(println "hummus")} 
             [:option {:value "0"} "Free"]
             [:option {:value "1"} "Something"]]]
       [:td
        [widgets/run-stop-toggle reagent-state [:run] "run-stop-toggle"]]]]])


(defn right-bar []
  [:div
   [:h3 "Controls"]      
   [widgets/simple-toggle reagent-state [:show-grid] "show-grid-toggle" "Show Grid"]
   [widgets/simple-toggle reagent-state [:show-axes] "show-axes-toggle" "Show Axes"]
   [widgets/simple-toggle reagent-state [:autorange] "autorange-toggle" "Auto Range"]      
   [widgets/text-entry reagent-state [:server-url] "Server URL" "http://localhost:3999/websocket" 30]
   [:div.signal-bar
    [:h3 "Signals"]
    [checkboxes]]])

(defn bottom-bar []
  (fn []
    [:div
     [:p
      (str @reagent-state)]]))

(defn init-reagent! []
  (r/render [top-bar]    (.getElementById js/document "reagent-top-bar"))
  (r/render [right-bar]  (.getElementById js/document "reagent-right-bar"))
  (r/render [bottom-bar] (.getElementById js/document "reagent-bottom-bar")))


;; -----------------------------------------------------------------------------
;; Now actually start everything!

(init-reagent!)

(anim/animate (fn [t] (draw-frame! t) true))
