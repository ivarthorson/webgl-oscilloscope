(ns webgl-oscilloscope.widgets)

;; HTML Widgets
;; Requires "widgets.css"

;; TODO: Consider replacing with
;; https://github.com/material-components/material-components-web/tree/master/packages/mdc-switch

(defn simple-toggle 
  "A toggle widget that will control the boolean at (get-in STATE-ATOM PATH-IN-STATE)."
  [state-atom path-in-state unique-id text-label]
  (fn []
    (let [s @state-atom]
      [:table
       [:tbody
        [:tr
         [:td 
          [:input {:class "tgl tgl-light"
                   :id unique-id
                   :type "checkbox"
                   :checked (get-in s path-in-state)
                   :on-click #(swap! state-atom update-in path-in-state (fn [x] (not x)))}]
          [:label {:class "tgl-btn"
                   :for unique-id
                   :data-tg-off "STOP"
                   :data-tg-on "RUN"}]]
         [:td {:width "5px"}]
         [:td text-label]]]])))

(defn run-stop-toggle
  "Like simple-toggle, but skewed and shows 'RUN' in green, and 'STOP' in red."
  [state-atom path-in-state unique-id]
  (fn []
    (let [s @state-atom]
      [:div
       [:input {:class "tgl tgl-skewed"
                :id unique-id 
                :type "checkbox"
                :checked (get-in s path-in-state)
                :on-click #(swap! state-atom update-in path-in-state (fn [x] (not x)))}]
       [:label {:class "tgl-btn"
                :for unique-id
                :data-tg-off "STOP"
                :data-tg-on "RUN"}]])))



(defn text-entry
  "A text entry widget, styled beautifully.
  https://material-components.github.io/material-components-web-catalog/#/component/text-field?type=outlined"
  [state-atom path-in-state text-label default-value width]
  [:div
   [:label text-label]
   [:input {:type "text"
            :class "userInput"
            :name "formula1"
            :default-value (or default-value "")
            :size (str (or width 10))
            :on-change #(swap! state-atom assoc-in path-in-state (-> % .-target .-value))}]])
#_
(defn slider
  "A range slider widget."
  [state-atom path-in-state min max step default-value]
  [:input {:type "range"
           ;;:value value
           :default-value default-value
           :min min 
           :max max
           :step step
           :style {:width "100%"}
           :on-change #(swap! state-atom assoc-in path-in-state (.> % .-target .-value))}])
