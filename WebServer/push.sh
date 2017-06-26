#!/bin/sh
gradle fatJar
rsync ./build/libs/WebServer-all-3.4.1.jar fhem:/home/pi/Server/WebServer-all.jar
