package webserver.ruleCheck;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleInfo;
import webserver.ruleCheck.rules.RuleState;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class keeps a state which can be applied to a FHEM model.
 *
 * @author Rafael
 */
class State {
    /**
     * Map a sensor name to the rules it passes or violates.
     * The key is a string because the model may reload at any moment
     * (the sensor may or may not change, but the name will stay the same or be
     * {@link webserver.ruleCheck.State#prune(FHEMModel) pruned}).
     */
    private final Map<String, Map<Rule, RuleInfo>> stateMap = new HashMap<>();

    /**
     * Update the static state with new results from an evaluation.
     * @param states the new results
     */
    void update(Set<RuleState> states) {

        for (RuleState state : states) {

            Rule rule = state.getRule();

            Set<FHEMSensor> passedSensors = state.getPassedSensors();

            for (FHEMSensor sensor : passedSensors) {
                if (!stateMap.containsKey(sensor.getName())) {
                    Map<Rule, RuleInfo> newSensorMap = new HashMap<>();
                    newSensorMap.put(rule, new RuleInfo(
                            rule.getName(),
                            true,
                            rule.getPermissionField(),
                            rule.getOkMessage(),
                            rule.getRelatedLogs(),
                            rule.getPriority()
                    ));
                    stateMap.put(sensor.getName(), newSensorMap);
                    continue;
                }

                Map<Rule, RuleInfo> sensorRules = stateMap.get(sensor.getName());

                if (!sensorRules.containsKey(rule)) {
                    RuleInfo ruleInfo = new RuleInfo(
                            rule.getName(),
                            true,
                            rule.getPermissionField(),
                            rule.getOkMessage(),
                            rule.getRelatedLogs(),
                            rule.getPriority()
                            );
                    sensorRules.put(rule, ruleInfo);
                    continue;
                }

                RuleInfo ruleInfo = sensorRules.get(rule);
                ruleInfo.setOk();
                ruleInfo.setMessage(rule.getOkMessage());
            }

            Set<FHEMSensor> violatedSensors = state.getViolatedSensors();

            for (FHEMSensor sensor : violatedSensors) {
                if (!stateMap.containsKey(sensor.getName())) {
                    Map<Rule, RuleInfo> newSensorMap = new HashMap<>();
                    newSensorMap.put(rule, new RuleInfo(
                            rule.getName(),
                            false,
                            rule.getPermissionField(),
                            rule.getWarningMessage(Instant.now().getEpochSecond()),
                            rule.getRelatedLogs(),
                            rule.getPriority()
                    ));
                    stateMap.put(sensor.getName(), newSensorMap);
                    continue;
                }

                Map<Rule, RuleInfo> sensorRules = stateMap.get(sensor.getName());

                if (!sensorRules.containsKey(rule)) {
                    RuleInfo ruleInfo = new RuleInfo(
                            rule.getName(),
                            false,
                            rule.getPermissionField(),
                            rule.getWarningMessage(Instant.now().getEpochSecond()),
                            rule.getRelatedLogs(),
                            rule.getPriority()
                    );
                    sensorRules.put(rule, ruleInfo);
                    continue;
                }

                RuleInfo ruleInfo = sensorRules.get(rule);
                ruleInfo.setNotOk();
                ruleInfo.setMessage(rule.getWarningMessage(ruleInfo.getLastStamp()));
            }
        }
    }

    /**
     * Remove sensors which are not in the given FHEM model from the static state.
     * @param model the model to use to check for sensors
     */
    private void prune(FHEMModel model) {
        Set<String> sensorNames = stateMap.keySet();
        sensorNames.removeIf(s -> !model.getSensorByName(s).isPresent());
    }

    /**
     * Attach RuleInfos to all sensors with current warning messages.
     *
     * @param model the model which should be annotated. RuleInfos will be added for the sensors.
     */
    void apply(FHEMModel model) {
        prune(model);
        for (String sensorName : stateMap.keySet()) {
            FHEMSensor sensor = model.getSensorByName(sensorName)
                    .orElseThrow(() -> new RuntimeException("Impossible! stateMap was just pruned..."));
            Set<RuleInfo> shownInApp = stateMap.get(sensorName).keySet().stream().filter(Rule::isVisible)
                    .map(rule -> stateMap.get(sensorName).get(rule)).collect(Collectors.toSet());
            sensor.addRuleInfos(shownInApp);
        }
    }

    /**
     * Reset the static state.
     * Useful for unit tests.
     */
    void clear() {
        stateMap.clear();
    }
}
