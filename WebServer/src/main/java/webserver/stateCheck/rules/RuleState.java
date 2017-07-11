package webserver.stateCheck.rules;

import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.util.Set;

/**
 * @author Rafael
 */

public class RuleState {
    private final Set<FHEMSensor> okSensors;
    private final Set<FHEMSensor> violatedSensors;
    /**
     * A Rule only has state true if no sensors violate it
     */
    private boolean state;

    RuleState(boolean state, Set<FHEMSensor> okSensors, Set<FHEMSensor> violatedSensors) {
        this.state = state;
        this.okSensors = okSensors;
        this.violatedSensors = violatedSensors;

    }

    public Set<FHEMSensor> getOkSensors() {
        return okSensors;
    }

    public Set<FHEMSensor> getViolatedSensors() {
        return violatedSensors;
    }

    public boolean isOk() {
        return state;
    }
}
