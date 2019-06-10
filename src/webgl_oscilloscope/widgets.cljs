(ns webgl-oscilloscope.widgets)

;; Simple HTML Widgets that have no dependencies except "widgets.css"

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
                   :on-change #(swap! state-atom update-in path-in-state (fn [x] (not x)))}]
          [:label {:class "tgl-btn"
                   :for unique-id}]]
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
                :on-change #(swap! state-atom update-in path-in-state (fn [x] (not x)))}]
       [:label {:class "tgl-btn"
                :for unique-id
                :data-tg-off "STOP"
                :data-tg-on "RUN"}]])))

(defn text-input
  [state-atom path-in-state text-label width]
  [:div.text-input-block
   [:label text-label ": "]
   [:input {:type "text"
            :class "text-input"
            :value (get-in @state-atom path-in-state)
            :size (str (or width 10))
            :on-change #(swap! state-atom assoc-in path-in-state (-> % .-target .-value))}]])

(defn range-input
  [state-atom path-in-state text-label min max step default-value]
  [:div.range-input-block
   [:label text-label ": "]
   [:input {:type "range"
            :min min 
            :max max
            :step step
            :value (get-in @state-atom path-in-state default-value)
            :style {:width "100%"}
            :on-change #(swap! state-atom assoc-in path-in-state (-> % .-target .-value))}]])

(defn number-input
  [state-atom path-in-state text-label min max step default-value]
  [:div.number-input-block
   [:label text-label ": "]
   [:input {:type "number"
            :min min
            :max max
            :step step
            :class "number-input"
            :value (get-in @state-atom path-in-state default-value)
            :on-change #(swap! state-atom assoc-in path-in-state (js/Number (-> % .-target .-value)))}]])

(defn tabbed-pages
  "Tabs across the top, and immediately below are the contents.
  TAB-DEFINITION-HASH has KEYS (must be strings!) that are the displayed
  tab headings, and values that are the hiccup HTML contents to display."
  [state-atom path-in-state unique-id tab-definition-hash]
  (fn []
    (let [s @state-atom
          k-selected (get-in s path-in-state)]
      [:div.tabbed-pages
       ;; The tab selection bar comes first
       [:div.tabbed-pages-topbar
        (for [k (keys tab-definition-hash)]
          ^{:key (str "tabbed-sidebar-" unique-id "-tab-" k)}
          [:div {:class (if (= k k-selected)
                          "w3-third tablink w3-bottombar w3-hover-light-grey w3-padding w3-border-red"
                          "w3-third tablink w3-bottombar w3-hover-light-grey w3-padding")
                 :on-click #(swap! state-atom assoc-in path-in-state k)}
           k])]

       ;; Next comes the tab contents.
       ;; Contents are always in HTML but are not always displayed.        
       [:div.tabbed-pages-contents
        (for [k (keys tab-definition-hash)]
          (let [id (str "tabbed-sidebar-" unique-id "-contents-" k)
                v (get tab-definition-hash k)]
            ^{:key id}
            [:div {:id id :style {:display (if (= k k-selected) "block" "none")}}
             v]))]])))

