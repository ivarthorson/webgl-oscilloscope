(ns webgl-oscilloscope.core
  (:require [thi.ng.geom.vector :as vec]
            [thi.ng.geom.gl.webgl.animator :as anim]
            [reagent.core :as r]
            [webgl-oscilloscope.color :as color]
            [webgl-oscilloscope.webgl :as webgl]
            [webgl-oscilloscope.widgets :as widgets]))

(def reagent-state 
  (r/atom {:server-url "http://localhost:3449"
                            
           :chans [{:source "Demo" :signal "Sine" :checked true}
                   {:source "Demo" :signal "Square" :checked true}
                   {:source "Demo" :signal "Triangle"}]
                            
           :show-axes true
           :show-grid true
           :show-tics true

           :right-bar-tab "Signals"
           :run true
           }))

(def traces (atom []))


(defn signal-list
  []
  (fn []
    (let [s @reagent-state]
      [:div.signal-list ;;{:style {:overflow-y "scroll" :height "400px"}}
       [:table {:width "100%"}
        [:tbody
         (loop [chans (map-indexed vector (:chans s))
                rows []]
           (if (empty? chans)
             rows
             (let [[idx {:keys [source signal checked] :as c}] (first chans)]
               (recur (next chans)
                      (concat rows 
                              [^{:key (str "checkbox-chan-" idx) }
                               [:tr
                                [:td
                                 [:label.eye-checkbox
                                  [:input {:type "checkbox"
                                           :checked checked
                                           :on-change #(swap! reagent-state assoc-in [:chans idx :checked] (not checked))}]
                                  [:font {:class (str "eye-checkbox-label")
                                          :style {:color (if checked
                                                           (color/color-idx idx)
                                                           (color/light-gray))} }
                                   (str source " / " signal)]]]
                                [:td {:align "right"} 
                                 [widgets/number-input reagent-state [:chans idx :position]
                                  "pos"
                                  -1E100 1E100 1.0 0.0]]
                                [:td
                                 [:button.tiny-button "+"]
                                 [:button.tiny-button "-"]
                                 [:button.tiny-button "0"]]
                                [:td {:row-span "2"}
                                 [:button.tiny-button "AUTO"]]]
                    
                               ;; Second row
                               ^{:key (str "checkbox-chan-extra-" idx) }
                               [:tr 
                                [:td ]
                                [:td {:align "right"} 
                                 [widgets/number-input reagent-state [:chans idx :scale]
                                  "scl"
                                  1e-100 1e100 1.0 1.0]]
                                [:td
                                 [:button.tiny-button "+"]
                                 [:button.tiny-button "-"]
                                 [:button.tiny-button "1"]]]])))
             )
           )]]])))

(defn top-bar
  [user-input]
  [:table
   [:tbody
    [:tr
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Reset all controls")}
           "Reset"]]    
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Clear history")}
           "Clear"]]
     [:td {:width "30px"}]
     [:td "Save:"]
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Save CSV of traces")}
           "Trace"]]
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Save PNG of traces")}
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
     [signal-list]
     
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

(defonce unused-reagent-handle (init-reagent!))

(defonce unused-animate-handle 
  (anim/animate (fn [t] 
                  (let [rs @reagent-state]
                    (webgl/draw-frame! rs t)) true)))



