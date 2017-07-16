package webserver.stateCheck;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.stateCheck.rules.Rule;
import webserver.stateCheck.rules.RuleInfo;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class keeps an intermediate state which can later be applied to a FHEM model.
 *
 * @author Rafael
 */
class State {
    /* Sensorname -> (Rulename, StartTime) */
    final Map<String, Map<String, Long>> violatedRules = new HashMap<>();
    final Map<String, Map<String, Long>> okRules = new HashMap<>();

    /**
     * Update the warning message and attach a RuleInfo to the sensor.
     *
     * @param model the model which should be checked. RuleInfos will be added for the sensors.
     * @param violating the set of violating rules which were evaluated
     * @param passed the set of passed rules which were evaluated
     */
    void setRuleInfos(FHEMModel model, Set<Rule> violating, Set<Rule> passed) {
        for (Iterator<FHEMSensor> it = model.eachSensor(); it.hasNext(); ) {
            FHEMSensor sensor = it.next();

            /* Get the names of the rules which the sensor already violated last time */
            Map<String, Long> violatedRulesOfSensor = violatedRules.get(sensor.getName());
            /* If sensor even has violating rules in the violatedRules map */
            if (violatedRulesOfSensor != null) {
                Set<String> oldViolatingRuleNames = violatedRulesOfSensor.keySet();

                Set<Rule> currentViolatingRules = violating.stream().
                        filter(s -> oldViolatingRuleNames.contains(s.getName())).collect(Collectors.toSet());
                for (Rule rule : currentViolatingRules) {
                    long timestamp = violatedRules.get(sensor.getName()).get(rule.getName());
                    String message = rule.getWarningMessage(timestamp);
                    sensor.addViolatedRule(new RuleInfo(rule.getName(), false, Instant.now().getEpochSecond(), rule.getPermissionField(), message));
                }
            }
        }
    }
}
