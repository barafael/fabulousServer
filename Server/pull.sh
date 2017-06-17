#!/bin/sh
rm -rf ~/fhemlog
rsync -r fhem:/opt/fhem/log/* ~/fhemlog/
ssh -l pi fhem "sudo -u fhem perl /opt/fhem/fhem.pl localhost:7072 \"jsonList2\" > jsonList2.json"
# location of mesp git dir is assumed currently, change this or maybe fix it with a global variable
rsync fhem:/home/pi/jsonList2.json ~/Uni/4.Semester/MESP/fabulousServer/Server/jsonList2.json
#sed -i "s/\/opt\/fhem\/log\//~\/fhemlog\//g" ~/Uni/4.Semester/MESP/fabulousServer/Server/jsonList2.json
#sed -i "s/\.\/log\//~\/fhemlog\//g" ~/Uni/4.Semester/MESP/fabulousServer/Server/jsonList2.json
