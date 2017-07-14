#!/bin/sh
gradle fatJar
rsync ./build/libs/WebServer-all-3.4.1.jar fhem:/home/pi/Server/webserver-all.jar
rsync ./rules.json fhem:Server/rules.json
