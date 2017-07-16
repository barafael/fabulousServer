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

    void update(Set<RuleState> states) {

        for (RuleState state : states) {

            Rule rule = state.getRule();

            Set<FHEMSensor> passedSensors = state.getPassedSensors();

            for (FHEMSensor sensor : passedSensors) {
                if (!stateMap.containsKey(sensor.getName())) {
                    Map<Rule, RuleInfo> newSensorMap = new HashMap<>();
                    newSensorMap.put(rule, new RuleInfo(
                            rule.getName(), true, rule.getPermissionField(), rule.getOkMessage()
                    ));
                    stateMap.put(sensor.getName(), newSensorMap);
                    continue;
                }

                Map<Rule, RuleInfo> sensorRules = stateMap.get(sensor.getName());

                if (!sensorRules.containsKey(rule)) {
                    RuleInfo ruleInfo = new RuleInfo(
                            rule.getName(), true, rule.getPermissionField(), rule.getOkMessage());
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
                            rule.getWarningMessage(Instant.now().getEpochSecond())
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
                            rule.getWarningMessage(Instant.now().getEpochSecond())
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
            sensor.addRuleInfos(stateMap.get(sensorName).values());
        }
    }

    void report(FHEMModel model) {
        prune(model);
        for (String sensorName : stateMap.keySet()) {
            FHEMSensor sensor = model.getSensorByName(sensorName)
                    .orElseThrow(() -> new RuntimeException("Impossible! stateMap was just pruned..."));
            // TODO implement event log
            System.out.println(sensor);
        }
    }
}
