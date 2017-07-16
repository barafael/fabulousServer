package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.util.Set;

/**
 * This class holds information about the state of a rule in the form of sets of ok/violated sensors.
 * The state should only be ok if violatedSensors is empty but okSensors is not.
 *
 * @author Rafael
 */
public final class RuleState {
    /**
     * A set of sensors for which this rule holds.
     */
    private final Set<FHEMSensor> okSensors;
    /**
     * A set of sensors for which this rule does not hold.
     */
    private final Set<FHEMSensor> violatedSensors;
    /**
     * A Rule only has state true if no sensors violate it.
     */
    private final boolean state;

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
