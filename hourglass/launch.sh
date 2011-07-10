#!/bin/bash
cd `dirname $0`
java -Djava.library.path=libs/opengl/:libs/serial/ -classpath "libs/minim/jsminim.jar:libs/minim/minim.jar:libs/minim/jl1.0.jar:libs/minim/tritonus_aos.jar:libs/minim/tritonus_share.jar:libs/minim/minim-spi.jar:libs/minim/mp3spi1.9.4.jar:libs/serial/serial.jar:libs/serial/RXTXcomm.jar:libs/fullscreen/fullscreen.jar:libs/processing/core.jar:libs/opengl/opengl.jar:libs/opengl/jogl.jar:libs/opengl/gluegen-rt.jar:libs/jbox2d/jbox2d-2.0.1-library-only.jar:bin" com.cefn.time.hourglass.Box2dApp
