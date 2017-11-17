#!/bin/sh
gradle fatJar
rsync ./build/libs/WebServer-all-3.4.1.jar innoLabControl:/home/pi/Server/webserver-all.jar
ssh innoLabControl "sudo systemctl restart innoLabServer.service"
