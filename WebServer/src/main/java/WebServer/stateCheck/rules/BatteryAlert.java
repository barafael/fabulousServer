package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.stateCheck.State;
import WebServer.stateCheck.VAR;
import WebServer.stateCheck.WARNINGLEVEL;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Rafael
 */

public class BatteryAlert extends Rule {
    public BatteryAlert() {
        super();
        // TODO: what about translation?
        name = "Fenster offen außerhalb der Öffnungszeiten";
        permission = "S_Fenster";
        okMessage = "Die Fenster sind nicht außerhalb der Öffnungszeiten geöffnet.";

        errorMessages.put(WARNINGLEVEL.LOW, "Die Fenster sind offen, aber das Lab ist geschlossen!");
        errorMessages.put(WARNINGLEVEL.MEDIUM, "Medium warning level!!! Fenster sind noch immer offen!");
        errorMessages.put(WARNINGLEVEL.HIGH, "Alert! Die Fenster im Lab wurden noch immer nicht geschlossen!");
        errorMessages.put(WARNINGLEVEL.DISASTER, "Disaster! Auch nach X Stunden wurden die Fenster im Lab nicht geschlossen.");

        sensors.add("HM_56A1EC");
        sensors.add("HM_56A1E8");
        sensors.add("HM_56A439");
        sensors.add("HM_56A27C");
    }

    @Override
    public boolean eval(FHEMModel model, State globalState) {
        Set<FHEMSensor> batterySensors = model.getSensorsByList(sensors);
        boolean state = true;
        for (FHEMSensor sensor : batterySensors) {
            Optional<Double> value = sensor.getBatteryValue();
            if (value.isPresent()) {
                double v = value.get();
                if (v < 25.0) {
                    affectedSensors.add(sensor.getName());
                    state = false;
                }
            }
        }
        newState(state);
        return state;
    }
}
