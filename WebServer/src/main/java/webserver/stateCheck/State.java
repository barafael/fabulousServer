package webserver.stateCheck;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.stateCheck.rules.Rule;
import webserver.stateCheck.rules.RuleInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class keeps an intermediate state which can later be applied to a FHEM model
 * @author Rafael
 */
class State {
    /* Sensorname -> (Rulename, StartTime) */
    Map<String, Map<String, Long>> state = new HashMap<>();

    /**
     * Update the warning message and attach a RuleInfo to the sensor.
     *
     * @param model the model which should be checked. RuleInfos will be added for the sensors.
     * @param rules the set of rules which should be evaluated
     */
    public void setRuleInfos(FHEMModel model, Set<Rule> rules) {
        for (Iterator<FHEMSensor> it = model.eachSensor(); it.hasNext(); ) {
            FHEMSensor sensor = it.next();

            Map<String, Long> allRulesOfSensor = state.get(sensor.getName());
            if (allRulesOfSensor != null) {
                Set<String> violatingRuleNames = allRulesOfSensor.keySet();

                Set<Rule> violatingRules = rules.stream().
                        filter(s -> violatingRuleNames.contains(s.getName())).collect(Collectors.toSet());
                for (Rule rule : violatingRules) {
                    long timestamp = state.get(sensor.getName()).get(rule.getName());
                    String message = rule.getWarningMessage(timestamp);
                    sensor.addRuleInfo(new RuleInfo(rule.getName(), rule.getPermissionField(), message));
                }
            }
        }
    }
}
