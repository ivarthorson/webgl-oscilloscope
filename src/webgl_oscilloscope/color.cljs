(ns webgl-oscilloscope.color)

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
  (-> [["#ef2929" "#cc0000" "#a40000"]   ; red
       ["#fcaf3e" "#f57900" "#ce5c00"]   ; orange
       ["#fce94f" "#edd400" "#c4a000"]   ; butter
       ["#8ae234" "#73d216" "#4e9a06"]   ; green
       ["#729fcf" "#3465a4" "#204a87"]   ; blue
       ["#ad7fa8" "#75507b" "#5c3566"]   ; plum
       ["#e9b96e" "#c17d11" "#8f5902"]]  ; chocolate
      (nth (mod idx 7)) 
      (nth (mod (rem idx 7) 3))))

(defn char2int
  [c]
  (condp = c
    "0" 0
    "1" 1
    "2" 2
    "3" 3
    "4" 4
    "5" 5
    "6" 6 
    "7" 7
    "8" 8
    "9" 9
    "a" 10
    "b" 11
    "c" 12
    "d" 13
    "e" 14
    "f" 15
    nil))

(defn hex2rgba
  "Assumes first character is a # and then 6 characters are hex."
  [s]
  (let [[_ d1 d2 d3 d4 d5 d6 ] (mapv (fn [c] (char2int (str c))) (rest s))
        r (/ (+ (* 16 d1) d2) 255.0)
        g (/ (+ (* 16 d3) d4) 255.0)
        b (/ (+ (* 16 d5) d6) 255.0)]
    [r b g 1.0]))

(defn color-rgba
  "Returns a color given an index number."
  [idx]
  (hex2rgba (color-idx idx)))

