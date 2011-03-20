#!/bin/sh
cd `dirname $0`
/media/BTEXT3/preventsleep.sh
java -Djava.library.path=./hindsight/libs/serial/library -cp ./hindsight/bin:./hindsight/libs/gsvideo_backported_20110304.jar:./hindsight/libs/gstreamer-java.jar:./hindsight/libs/fullscreen/library/fullscreen.jar:./hindsight/libs/serial/library/RXTXcomm.jar:./hindsight/libs/serial/library/serial.jar:./hindsight/libs/core.jar:./hindsight/libs/jna.jar com.cefn.time.hindsight.HindsightApp
