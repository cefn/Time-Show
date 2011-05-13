#!/bin/bash
java -Djava.library.path=libs/opengl/:libs/serial/ -classpath libs/serial/serial.jar:libs/serial/RXTXcomm.jar:libs/fullscreen/library/fullscreen.jar:libs/processing/core.jar:libs/opengl/opengl.jar:libs/opengl/jogl.jar:libs/opengl/gluegen-rt.jar:libs/jbox2d/jbox2d-2.0.1-library-only.jar:bin com.cefn.time.hourglass.Box2dApp
