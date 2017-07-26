package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.time.Instant;
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
    /**
     * Whether the corresponding rule was ok.
     */
    private final boolean isOk;

    /**
     * Construct a rule state given a rule, passed, and violated sensors.
     * The state is set to ok if there were passed but no violated sensors.
     *
     * @param rule            the evaluated rule
     * @param passedSensors   the passed sensors
     * @param violatedSensors the violated sensors
     */
    RuleState(Rule rule, Set<FHEMSensor> passedSensors, Set<FHEMSensor> violatedSensors) {
        this.rule = rule;
        this.passedSensors = passedSensors;
        this.violatedSensors = violatedSensors;
        isOk = !passedSensors.isEmpty() && violatedSensors.isEmpty();
    }

    /**
     * Specific constructor for general predicate.
     * This is necessary because the state of a general predicate is not dependent on it's
     * passed or violated rules.
     * Instead, the general predicate is evaluated independently from the input,
     * and it's state is set in this constructor.
     *
     * @param ruleOK           whether the rule passed
     * @param generalPredicate the general predicate which was evaluated
     */
    public RuleState(boolean ruleOK, GeneralPredicate generalPredicate) {
        isOk = ruleOK;
        this.rule = generalPredicate;
        passedSensors = new HashSet<>();
        violatedSensors = new HashSet<>();
    }

    /**
     * Specific constructor for meta predicate.
     * This is necessary because the state of a meta predicate is not dependent on it's
     * passed or violated rules.
     * Instead, the meta predicate is evaluated only depending on requiredtrue/requiredfalse rules.
     *
     * @param ruleOK        whether the rule passed
     * @param metaPredicate the meta predicate which was evaluated
     */
    public RuleState(boolean ruleOK, Meta metaPredicate) {
        isOk = ruleOK;
        this.rule = metaPredicate;
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
