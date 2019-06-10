(ns webgl-oscilloscope.core
  (:require [thi.ng.geom.vector :as vec]
            [thi.ng.geom.gl.webgl.animator :as anim]
            [reagent.core :as r]
            [webgl-oscilloscope.color :as color]
            [webgl-oscilloscope.webgl :as webgl]
            [webgl-oscilloscope.widgets :as widgets]))

(def reagent-state 
  (r/atom {:server-url "http://localhost:3449"
                            
           :chans [{:source "Demo" :signal "Sine" :checked true :position 0 :scale 1.0}
                   {:source "Demo" :signal "Square" :checked true}
                   {:source "Demo" :signal "Triangle"}]
                            
           :show-axes true
           :show-grid true
           :show-tics true

           :right-bar-tab "Signals"

           :run true
           }))

(def traces (atom []))

(defn signal-list []
  (let [s @reagent-state]
    [:div.signal-list ;;{:style {:overflow-y "scroll" :height "400px"}}
     [:table {:width "100%"}
      [:tbody
       (loop [chans (map-indexed vector (:chans s))
              rows []]
         (if (empty? chans)
           rows
           (let [[idx {:keys [source signal] :as c}] (first chans)]
             (recur (next chans)
                    (concat rows 
                            [;; First row
                             ^{:key (str "checkbox-chan-name-row" idx) }
                             [:tr
                              [:td {:col-span "6"}
                               [:label.eye-checkbox
                                [:input {:type "checkbox"
                                         :checked (get-in @reagent-state [:chans idx :checked] false)
                                         :on-change #(swap! reagent-state update-in [:chans idx :checked] (fn [x] (not x)))}]
                                [:font {:class (str "eye-checkbox-label")
                                        :style {:color (if (get-in @reagent-state [:chans idx :checked] false)
                                                         (color/color-idx idx)
                                                         (color/light-gray))} }
                                 (str source " / " signal)]]]]
                             
                             ;; Second row
                             ^{:key (str "checkbox-chan-control-row-" idx) }
                             [:tr
                              [:td {:width "15px"}]
                              [:td {:align "right"} 
                               [widgets/number-input reagent-state [:chans idx :position]
                                "pos" -1E100 1E100 1.0 0.0]]
                              [:td
                               [:button.tiny-button {:on-click #(swap! reagent-state update-in [:chans idx :position] (partial +  1))} "+"]
                               [:button.tiny-button {:on-click #(swap! reagent-state update-in [:chans idx :position] (partial + -1))} "-"]
                               [:button.tiny-button {:on-click #(swap! reagent-state assoc-in [:chans idx :position] 0)} "0"]]
                              [:td {:align "right"}
                               [widgets/number-input reagent-state [:chans idx :scale]
                                "scl" 1e-100 1e100 1.0 1.0]]
                              [:td
                               [:button.tiny-button {:on-click #(swap! reagent-state update-in [:chans idx :scale] (partial * 2.0))} "+"]
                               [:button.tiny-button {:on-click #(swap! reagent-state update-in [:chans idx :scale] (partial * 0.5))} "-"]
                               [:button.tiny-button {:on-click #(swap! reagent-state assoc-in [:chans idx :scale] 1.0)} "1"]]
                              [:td 
                               [:button.tiny-button "Auto"]]]])))))]]]))

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
     [:td {:width "10px"}]
     [:td "Save:"]
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Save CSV of traces")}
           "Trace"]]
     [:td [:button.oscope-button
           {:on-click #(print "TODO: Save PNG of traces")}
           "Image"]]
     [:td {:width "10px"}]
    #_ [:td "Cursor: "
      [widgets/dropdown-input reagent-state [:cursor] "cursor-mode-dropdown" [["None" "None"]
                                                                              ["Track" "Track"]
                                                                              ["Horiz" "Horiz"]
                                                                              ["Vert" "Vert"]]]]
     [:td {:align "right"} 
      [widgets/number-input reagent-state [:x-position]
       "Time Lag" -1E100 1E100 1.0 0.0]]
     [:td
      [:button.tiny-button {:on-click #(swap! reagent-state update-in [:x-position] (partial +  1))} "+"]
      [:button.tiny-button {:on-click #(swap! reagent-state update-in [:x-position] (partial + -1))} "-"]
      [:button.tiny-button {:on-click #(swap! reagent-state assoc-in [:x-position] 0)} "0"]]
     [:td {:align "right"}
      [widgets/number-input reagent-state [:x-scale]
       "Hist" 0.1 1000 1.0 1.0]]
     [:td
      [:button.tiny-button {:on-click #(swap! reagent-state update-in [:x-scale] (partial * 2.0))} "+"]
      [:button.tiny-button {:on-click #(swap! reagent-state update-in [:x-scale] (partial * 0.5))} "-"]
      [:button.tiny-button {:on-click #(swap! reagent-state assoc-in [:x-scale] 1.0)} "1"]]
     
     [:td {:width "15px"}]
     [:td [widgets/run-stop-toggle reagent-state [:run] "run-stop-toggle"]]]]])


(defn right-bar []
  [:div.w3-container
   ;; The tab selection bar comes first
   [widgets/tabbed-pages reagent-state [:right-bar-tab] "right-bar-tabs"
    {"Signals"
     [:div 
      ;;[:h3 "Signals"]
      [signal-list]]
     
     "Measure" 
     "Measurement stuff here"

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

(defonce unused-animate-handle 
  (anim/animate (fn [t] 
                  (let [rs @reagent-state]
                    (webgl/draw-frame! rs t)) true)))



