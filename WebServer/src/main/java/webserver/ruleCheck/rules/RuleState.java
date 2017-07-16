package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.util.HashSet;
import java.util.Set;

/**
 * This class holds information about the state of a rule in the form of sets of ok/violated sensors.
 * The state should only be ok if violatedSensors is empty but passedSensors is not.
 *
 * @author Rafael
 */
public final class RuleState {
    /**
     * The rule for which this state applies.
     */
    private final Rule rule;

    /**
     * A set of sensors for which this rule holds.
     */
    private final Set<FHEMSensor> passedSensors;

    /**
     * A set of sensors for which this rule does not hold.
     */
    private final Set<FHEMSensor> violatedSensors;
    private final boolean isOk;

    RuleState(Rule rule, Set<FHEMSensor> passedSensors, Set<FHEMSensor> violatedSensors) {
        this.rule = rule;
        this.passedSensors = passedSensors;
        this.violatedSensors = violatedSensors;
        isOk = !passedSensors.isEmpty() && violatedSensors.isEmpty();
    }

    public RuleState(boolean ruleOK, GeneralPredicate generalPredicate) {
        isOk = ruleOK;
        this.rule = generalPredicate;
        passedSensors = new HashSet<>();
        violatedSensors = new HashSet<>();
    }

    public Rule getRule() {
        return rule;
    }

    boolean isOk() {
        return isOk;
    }

    public Set<FHEMSensor> getPassedSensors() {
        return passedSensors;
    }

    public Set<FHEMSensor> getViolatedSensors() {
        return violatedSensors;
    }
}
