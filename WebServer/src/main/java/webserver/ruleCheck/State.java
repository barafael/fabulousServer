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
     *
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
                            rule.getViewPermissions(),
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
                            rule.getViewPermissions(),
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
                            rule.getViewPermissions(),
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
                            rule.getViewPermissions(),
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
     *
     * @param model the model to use to check for sensors
     */
    private void prune(FHEMModel model) {
        Set<String> sensorNames = stateMap.keySet();
        sensorNames.removeIf(s -> !model.getSensorByName(s).isPresent());
    }

    /**
     * Prune the stateMap, removing rules which are not present any more.
     *
     * @param rulesToKeep the new set of rules
     */
    public void prune(Set<Rule> rulesToKeep) {
        Set<String> namesToKeep = rulesToKeep.stream().map(Rule::getName).collect(Collectors.toSet());
        for (Map.Entry<String, Map<Rule, RuleInfo>> stringMapEntry : stateMap.entrySet()) {
            Map<Rule, RuleInfo> rulesOfSensor = stringMapEntry.getValue();
            for (Rule rule : rulesOfSensor.keySet()) {
                if (!rulesToKeep.contains(rule)) {
                    rulesOfSensor.remove(rule);
                }
            }
        }
    }

    /**
     * Attach RuleInfos to all sensors with current warning messages.
     *
     * @param model the model which should be annotated. RuleInfos will be added for the sensors.
     */
    void apply(FHEMModel model) {
        //prune(model);
        for (Map.Entry<String, Map<Rule, RuleInfo>> stringMapEntry : stateMap.entrySet()) {
            FHEMSensor sensor = model.getSensorByName(stringMapEntry.getKey())
                    .orElseThrow(() -> new RuntimeException("Impossible! stateMap was just pruned..."));
            Set<RuleInfo> shownInApp = stringMapEntry.getValue().keySet().stream().filter(Rule::isVisible)
                    .map(rule -> stringMapEntry.getValue().get(rule)).collect(Collectors.toSet());
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
