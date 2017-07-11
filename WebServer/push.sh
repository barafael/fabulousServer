#!/bin/sh
gradle fatJar
rsync ./build/libs/webserver-all-3.4.1.jar fhem:/home/pi/Server/webserver-all.jar
