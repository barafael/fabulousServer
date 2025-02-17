package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final String ruleName;

    /**
     * A set of sensors for which this rule holds.
     */
    private final Set<String> passedSensors;

    /**
     * A set of sensors for which this rule does not hold.
     */
    private final Set<String> violatedSensors;
    /**
     * Whether the corresponding rule was ok.
     */
    private final boolean isOk;
    private final Rule rule;

    private long stamp;

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
        this.ruleName = rule.getName();
        this.passedSensors = passedSensors.stream().map(FHEMSensor::getName).collect(Collectors.toSet());
        this.violatedSensors = violatedSensors.stream().map(FHEMSensor::getName).collect(Collectors.toSet());
        isOk = !passedSensors.isEmpty() && violatedSensors.isEmpty();
        stamp = Instant.now().getEpochSecond();
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
        this.rule = generalPredicate;
        isOk = ruleOK;
        this.ruleName = generalPredicate.getName();
        passedSensors = new HashSet<>();
        violatedSensors = new HashSet<>();
        stamp = Instant.now().getEpochSecond();
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
        this.rule = metaPredicate;
        isOk = ruleOK;
        this.ruleName = metaPredicate.getName();
        passedSensors = new HashSet<>();
        violatedSensors = new HashSet<>();
        stamp = Instant.now().getEpochSecond();
    }

    public Rule getRule() {
        return rule;
    }

    public Set<String> getPassedSensors() {
        return passedSensors;
    }

    public Set<String> getViolatedSensors() {
        return violatedSensors;
    }

    public boolean isOk() {
        return isOk;
    }

    public RuleState stamp() {
        stamp = Instant.now().getEpochSecond();
        return this;
    }

    public String getRuleName() {
        return ruleName;
    }

    public long getDuration() {
        return Instant.now().getEpochSecond() - stamp;
    }

    public long getLastStamp() {
        return stamp;
    }
}
