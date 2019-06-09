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

(def reagent-state (r/atom {:server-url "http://localhost:3449"
                            
                            :chans [{:id 0 :source "Demo" :signal "Sine" :checked true}
                                    {:id 1 :source "Demo" :signal "sig2" :checked true}
                                    {:id 2 :source "Demo" :signal "sig3"}
                                    {:id 3 :source "source2" :signal "sig5"}]
                            
                            :show-axes true
                            :show-grid true
                            :show-tics true

                            :right-bar-tab "Signals"
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

(def sine-wave (let [ts (range 0 314.0 0.01)
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
    
    (when (get-in rs [:show-axes])
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

;; <input type="checkbox" id="chk2" /> <label for="chk2">Some label</label>

(defn light-gray
  [& [highlight?]]
  (if highlight?
    "#eeeeec" 
    "#d3d7cf"))

(defn dark-gray
  [& [highlight?]]
  (if highlight?
    "#888a85"
    "#555753"))

(defn color-idx
  "Returns a color given an index number."
  [idx]
  (-> [["#ef2929" "#cc0000" "#a40000"] ; Red
        ["#fcaf3e" "#f57900" "#ce5c00"] ; orange
        ["#fce94f" "#edd400" "#c4a000"] ; butter
        ["#8ae234" "#73d216" "#4e9a06"] ; green
        ["#729fcf" "#3465a4" "#204a87"] ; blue
        ["#ad7fa8" "#75507b" "#5c3566"] ; plum
        ["#e9b96e" "#c17d11" "#8f5902"]]   ; chocolate
      (nth (mod idx 7)) 
      (nth (mod (rem idx 7) 3))))

(defn checkboxes
  []
  (let [s @reagent-state]
    [:div.signal-list ;;{:style {:overflow-y "scroll" :height "400px"}}
     (for [{:keys [id source signal checked] :as c} (:chans s)]
       ^{:key (str "checkbox-chan-" id) }
       [:div
        [:table {:width "100%"}
         [:tbody
          [:tr
           [:td
            [:label.eye-checkbox
             [:input {:type "checkbox"
                      :checked checked
                      :on-change #(swap! reagent-state assoc-in [:chans id :checked] (not checked))}]
             [:font {:class (str "eye-checkbox-label")
                     :style {:color (if checked
                                      (color-idx id)
                                      (light-gray))} }
              (str source " / " signal)]]]
           [:td {:align "right"} 
            [widgets/number-input reagent-state [:chans id :position]
             "pos"
             -1e100 1e100 1.0 0.0]]
           [:td
            [:button.tiny-button "+"]
            [:button.tiny-button "-"]
            [:button.tiny-button "0"]]
           [:td {:rowspan "2"}
            [:button.tiny-button "AUTO"]]]
          [:tr 
           [:td ]
           [:td {:align "right"} 
            [widgets/number-input reagent-state [:chans id :scale]
             "scl"
             1e-100 1e100 1.0 1.0]]
           [:td
            [:button.tiny-button "+"]
            [:button.tiny-button "-"]
            [:button.tiny-button "1"]]]]]])]))

(defn top-bar
  [user-input]
  [:table
   [:tbody
    [:tr
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Clear Everything")}
           "Reset"]]    
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Clear Everything")}
           "Clear"]]
     [:td {:width "30px"}]
     [:td "Save:"]
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Reset Y axis zoom")}
           "Trace"]]
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Reset Y axis zoom")}
           "Image"]]
     [:td {:width "30px"}]
     [:td "Cursor: "
      [:select {:class "dropdown-input"
                :default-value "0"
                :on-change #(println "Cursor changed")} 
       [:option {:value "0"} "None"]
       [:option {:value "1"} "Track"]
       [:option {:value "2"} "Horiz"]
       [:option {:value "3"} "Vert"]]]
     [:td {:width "30px"}]
     [:td [widgets/run-stop-toggle reagent-state [:run] "run-stop-toggle"]]]]])


(defn right-bar []
  [:div.w3-container
   ;; The tab selection bar comes first
   [widgets/tabbed-pages reagent-state [:right-bar-tab] "right-bar-tabs"
    {"Signals"  
     [checkboxes]
     
     ;; "Measure" 
     ;; "Measurement stuff here"

     "Viewport" 
     [:div
      [widgets/simple-toggle reagent-state [:show-axes] "show-axes-toggle" "Show Axes"]   
      [widgets/simple-toggle reagent-state [:show-grid] "show-grid-toggle" "Show Grid"]
      [widgets/simple-toggle reagent-state [:show-tics] "show-tics-toggle" "Show Tics"]
      
      ;; 
      "TODO: DROPDOWN FOR MODE"
      


      ]
     
     "Options" 
     "Options contents here"}]

#_
   [:div
    
    [widgets/range-input reagent-state [:horiz-position] "Horiz Pos." -10 10 0.01 0]
    [widgets/range-input reagent-state [:horiz-scale] "Horiz Scale" 0 1 0.01 4]

    [:button.oscope-button
          {:on-click #(print "TODO: Reset Y axis zoom")}
     "Position Out"]
     [:button.oscope-button
          {:on-click #(print "TODO: Reset Y axis zoom")}
          "Zoom Out"]
    
    [:h3 "Viewport"]      
   
    
    ;;[widgets/text-input reagent-state [:server-url] "Server URL"  20]
    ;;[widgets/text-input reagent-state [:server-url] "Update Rate" 20]
    
    ;;[widgets/range-input reagent-state [:time-lag] "Time Lag" 0 1 0.01 4]
    ;;[widgets/range-input reagent-state [:time-history] "History" 0 100 0.1 10]
    ]])

(defn bottom-bar []
  (fn []
    [:div.options
     "Connected to http://localhost:3000/websocket..."          
     ]))

(defn page-bottom []
  (fn []
    [:div     
     [:p
      (str @reagent-state)]]))

(defn init-reagent! []
  (r/render [top-bar]    (.getElementById js/document "reagent-top-bar"))
  (r/render [right-bar]  (.getElementById js/document "reagent-right-bar"))
  (r/render [bottom-bar] (.getElementById js/document "reagent-bottom-bar"))
  (r/render [page-bottom] (.getElementById js/document "reagent-page-bottom")))


;; -----------------------------------------------------------------------------
;; Now actually start everything!

(init-reagent!)

(anim/animate (fn [t] (draw-frame! t) true))



