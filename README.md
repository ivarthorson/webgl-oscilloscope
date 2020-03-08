# webgl-oscilloscope

An in-browser oscilloscope for displaying rapidly changing signals in a browser.

The concept is that a backend server (CLJ / Java) captures near-realtime, rapidly changing data data via one or more sockets, and then pushes the data out to one or more browsers via websockets. This lets you use one or more browser windows as oscilloscopes to monitor a multitude of signals.
   
## Status

- Unclear, as of 2020-03-08. I was working on this last year and life interrupted. I recall it graphed fine, but I hadn't quite finished the generic server input sockets, so it probably generates synthetic data right now.

## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not get live reloading, nor a REPL. 

## References

- https://github.com/asakeron/cljs-webgl.git
- https://github.com/adkelley/cljs-webgl-examples.git
- https://github.com/thi-ng/geom.git


## License

Copyright ©2019- Ivar Thorson. Distributed under the GNU Public License Version 3.
