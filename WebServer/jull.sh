#!/bin/sh
rm -rf ${FHEMMOCKDIR}/fhemlog/jsonList2.json
ssh fhem "sudo -u fhem perl /opt/fhem/fhem.pl localhost:7072 \"jsonList2\" > jsonList2.json"
rsync fhem:/home/pi/jsonList2.json ${FHEMMOCKDIR}/jsonList2.json
