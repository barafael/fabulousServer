package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.stateCheck.State;
import WebServer.stateCheck.VAR;
import WebServer.stateCheck.WARNINGLEVEL;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Rafael
 */

public class LightInTheNight extends Rule {
    private static final long BRIGHTNESS_THRESHHOLD = 5;

    public LightInTheNight() {
        super();
        // TODO: what about translation?
        name = "Batter Low Level Report";
        permission = "S_Bat";
        okMessage = "Batterien Ok";

        errorMessages.put(WARNINGLEVEL.LOW, "");
        errorMessages.put(WARNINGLEVEL.MEDIUM, "");
        errorMessages.put(WARNINGLEVEL.HIGH, "");
        errorMessages.put(WARNINGLEVEL.DISASTER, "");

        sensors.add("HM_56A1EC");
        sensors.add("HM_56A1E8");
        sensors.add("HM_56A439");
        sensors.add("HM_56A27C");
    }

    //TODO use affected sensors
    @Override
    public boolean eval(FHEMModel model, State globalState) {
        long currentTime = Instant.now().getEpochSecond();
        if (currentTime >= (globalState.getTimes().get(VAR.SUNRISE) - 1800)
                && currentTime <= (globalState.getTimes().get(VAR.ENDTIME) + 1800)) {
            newState(true);
            return true;
        }
        Set<FHEMSensor> brightnessSensors = model.getSensorsByList(sensors);
        List<Long> values = brightnessSensors.stream()
                .map(FHEMSensor::getStateValue)
                .map(Long::parseLong).collect(Collectors.toList());
        boolean state = true;
        for (Long value : values) {
            if (value > BRIGHTNESS_THRESHHOLD) {
                state = false;
            }
        }
        newState(state);
        return state;
    }
}
