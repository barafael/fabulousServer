#!/bin/sh
gradle clean
gradle fatJar
rsync ./build/libs/Server-all-0.1.0-SNAPSHOT.jar fhem:/home/pi/Server/Server-all.jar

