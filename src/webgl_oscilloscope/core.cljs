(ns webgl-oscilloscope.core
  (:require [thi.ng.geom.vector :as vec]
            [thi.ng.geom.gl.webgl.animator :as anim]
            [reagent.core :as r]
            [webgl-oscilloscope.color :as color]
            [webgl-oscilloscope.traces :as traces]
            [webgl-oscilloscope.webgl :as webgl]
            [webgl-oscilloscope.widgets :as widgets]))

(def reagent-state 
  (r/atom {:server-url "http://localhost:3449"

           :status-message "Connecting to http://localhost:3000/websocket..."
                            
           :chans [{:source "Demo" :signal "Sine"   :checked true}
                   {:source "Demo" :signal "Square"}
                   {:source "Demo" :signal "Triangle"}]
                            
           :show-axes true
           :show-grid true

           :right-bar-tab "Signals"

           :run true
           }))

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
                               [:button.tiny-button {:on-click #(swap! reagent-state update-in [:chans idx :position] 
                                                                       (partial +    (* 0.2 (get-in @reagent-state [:chans idx :scale ] 1.0))))} "+"]
                               [:button.tiny-button {:on-click #(swap! reagent-state update-in [:chans idx :position]
                                                                       (partial + (- (* 0.2 (get-in @reagent-state [:chans idx :scale ] 1.0)))))} "-"]
                               [:button.tiny-button {:on-click #(swap! reagent-state assoc-in [:chans idx :position] 0)} "0"]]
                              [:td {:align "right"}
                               [widgets/number-input reagent-state [:chans idx :scale]
                                "scl" 1e-100 1e100 1.0 1.0]]
                              [:td
                               [:button.tiny-button {:on-click #(swap! reagent-state update-in [:chans idx :scale] (fnil (partial * 2.0) 1.0))} "+"]
                               [:button.tiny-button {:on-click #(swap! reagent-state update-in [:chans idx :scale] (fnil (partial * 0.5) 1.0))} "-"]
                               [:button.tiny-button {:on-click #(swap! reagent-state assoc-in [:chans idx :scale] 1.0)} "1"]]
                              [:td 
                               [:button.tiny-button "Auto"]]]])))))]]]))

(defn top-bar
  [user-input]
  [:table
   [:tbody
    [:tr
     [:td [:button.oscope-button
           {:on-click #(swap! reagent-state assoc :t0 0.0)}
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
     [:td "Cursor: " [widgets/dropdown-input reagent-state [:cursor] "cursor-mode-dropdown" [["None" "None"]
                                                                                             ["Track" "Track"]
                                                                                             ["Horiz" "Horiz"]
                                                                                             ["Vert" "Vert"]]]]
     [:td {:width "10px"}]
     [:td {:align "right"} 
      [widgets/number-input reagent-state [:x-position]
       "Lag" -1E100 1E100 1.0 0.0]]
     [:td
      [:button.tiny-button {:on-click #(swap! reagent-state update-in [:x-position]
                                              (partial +    (* 0.2 (get-in @reagent-state [:x-scale ] 1))))} "+"]
      [:button.tiny-button {:on-click #(swap! reagent-state update-in [:x-position] 
                                              (partial + (- (* 0.2 (get-in @reagent-state [:x-scale ] 1)))))} "-"]
      [:button.tiny-button {:on-click #(swap! reagent-state assoc-in [:x-position] 0)} "0"]]
     [:td {:align "right"}
      [widgets/number-input reagent-state [:x-scale]
       "Hist" 0.1 1000 1.0 1.0]]
     [:td
      [:button.tiny-button {:on-click #(swap! reagent-state update-in [:x-scale] (fnil (partial * 2.0) 1.0))} "+"]
      [:button.tiny-button {:on-click #(swap! reagent-state update-in [:x-scale] (fnil (partial * 0.5) 1.0))} "-"]
      [:button.tiny-button {:on-click #(swap! reagent-state assoc-in  [:x-scale] 1.0)} "1"]]
     
     [:td {:width "10px"}]
     [:td [widgets/run-stop-toggle reagent-state [:run] "run-stop-toggle"]]
     [:td {:width "10px"}]
     ]]])


(defn right-bar []
  [:div.w3-container
   ;; The tab selection bar comes first
   [widgets/tabbed-pages reagent-state [:right-bar-tab] "right-bar-tabs"
    {"Signals"
     [:div      
      [signal-list]]
     
     "Measure"
     [:div
      "TODO: Measurements like mean, peak to peak height, RMS amplitude, period, freq, , "]

     "Viewport" 
     [:div
      [:h4 "Visual Aids"]
      [widgets/simple-toggle reagent-state [:show-axes] "show-axes-toggle" "Show Axes"]   
      [widgets/simple-toggle reagent-state [:show-grid] "show-grid-toggle" "Show Grid"]
      [widgets/number-input reagent-state [:grid-spacing]
       "Grid Spacing" 0.05 1000 0.05 0.2]
      [:hr]
      [:span 
       "Plot Mode: (TODO) " [widgets/dropdown-input reagent-state [:plot-mode]
                             "plot-mode-dropdown" [["Timeseries"  "Timeseries"]
                                                   ["X-Y Scatter" "X-Y Scatter"]
                                                   ["FFT" "FFT"]]]]]
     
     "Options" 
     [:div
      [:h4 "Connection Settings"]
      [widgets/text-input reagent-state [:server-url] "Server URL"  20]
      [widgets/number-input reagent-state [:server-refresh]
       "Refresh [Hz]" 0.1 100 1.0 10.0]
      [:hr]
      ]}]])

(defn bottom-bar []
  [:div (str (:status-message @reagent-state))])

(defn page-bottom []
  [:p (str @reagent-state)])

(defn init-reagent! []
  (r/render [top-bar]    (.getElementById js/document "reagent-top-bar"))
  (r/render [right-bar]  (.getElementById js/document "reagent-right-bar"))
  (r/render [bottom-bar] (.getElementById js/document "reagent-bottom-bar"))
  (r/render [page-bottom] (.getElementById js/document "reagent-page-bottom")))


;; -----------------------------------------------------------------------------
;; Now actually start everything!

(init-reagent!)

;; Temporary place to store the latest time
;; (def (js/globalArray ))

(defonce unused-animate-handle 
  (anim/animate (fn [t]
                  (let [rs @reagent-state]
                    
                    (webgl/draw-frame! rs (- t (get @reagent-state :t0 0.0)))) true)))



