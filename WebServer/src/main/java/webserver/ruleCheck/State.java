package webserver.ruleCheck;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.rules.GeneralPredicate;
import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleInfo;
import webserver.ruleCheck.rules.RuleState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    private final Map<String, Map<Rule, RuleInfo>> infoMap = new HashMap<>();
    private final Map<String, RuleState> stateMap = new HashMap<>();
    private History history = new History();
    private List<RuleSnapshot> snapshots = new ArrayList<>();

    /**
     * Update the static state with new results from an evaluation.
     *
     * @param states the new results
     */
    void update(Set<RuleState> states, Set<Rule> rules, FHEMModel model) {
        for (RuleState state : states) {

            Rule rule = rules.stream().filter(r -> r.getName().equals(state.getRuleName())).findAny().get();

            /* Ignore invisible rules and general predicates, which both should only serve as
             * requisites for real rules.
             */
            if (!rule.isVisible() || rule instanceof GeneralPredicate) {
                continue;
            }

            boolean isOk = state.isOk();

            if (!stateMap.containsKey(rule.getName())) {
                stateMap.put(rule.getName(), state);
                continue;
            }

            if (isOk) {
                /* Was the rule ok previously? */
                if (stateMap.get(rule.getName()).isOk()) {
                    /* If so, do nothing, the stamp is preserved. */
                    continue;
                }
                /* Else, the rule changed to ok in this evaluation.
                 * It has to be added as a new event.
                 */
                RuleEvent event = new RuleEvent(stateMap.get(rule.getName()), rule);
                history.add(event);
                /* Remove the !isOk RuleState and replace it */
                stateMap.put(rule.getName(), state);
            } else {
                /* The rule was not ok, there are violated sensors */
                if (stateMap.get(rule.getName()).isOk()) {
                    /* Previously, rule was ok.
                     * Replace it with freshly stamped notOk state.
                     */
                    stateMap.put(rule.getName(), state);
                } else {
                    /* Rule was not ok and has not changed, so don't do anything for now */
                    continue;
                }
            }
        }

        for (RuleState state : states) {

            Rule rule = rules.stream().filter(r -> r.getName().equals(state.getRuleName())).findAny().get();

            /* Ignore invisible rules and general predicates, which both should only serve as
             * requisites for real rules.
             */
            if (!rule.isVisible() || rule instanceof GeneralPredicate) {
                continue;
            }

            Set<FHEMSensor> passedSensors = model.getSensorsByCollection(state.getPassedSensors());

            if (!stateMap.containsKey(rule.getName())) {
                stateMap.put(rule.getName(), state);
                continue;
            }

            for (FHEMSensor sensor : passedSensors) {
                if (!infoMap.containsKey(sensor.getName())) {
                    Map<Rule, RuleInfo> newSensorMap = new HashMap<>();
                    newSensorMap.put(rule, new RuleInfo(
                            rule.getName(),
                            true,
                            rule.getViewPermissions(),
                            rule.getOkMessage(),
                            rule.getRelatedLogs(),
                            rule.getPriority()
                    ));
                    infoMap.put(sensor.getName(), newSensorMap);
                    continue;
                }

                Map<Rule, RuleInfo> sensorRules = infoMap.get(sensor.getName());

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

            Set<FHEMSensor> violatedSensors = model.getSensorsByCollection(state.getViolatedSensors());

            for (FHEMSensor sensor : violatedSensors) {
                if (!infoMap.containsKey(sensor.getName())) {
                    Map<Rule, RuleInfo> newSensorMap = new HashMap<>();
                    newSensorMap.put(rule, new RuleInfo(
                            rule.getName(),
                            false,
                            rule.getViewPermissions(),
                            rule.getWarningMessage(Instant.now().getEpochSecond()),
                            rule.getRelatedLogs(),
                            rule.getPriority()
                    ));
                    infoMap.put(sensor.getName(), newSensorMap);
                    continue;
                }

                Map<Rule, RuleInfo> sensorRules = infoMap.get(sensor.getName());

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
            }
        }
        updateSnapshot(rules);
    }

    /**
     * Remove sensors which are not in the given FHEM model from the static state.
     *
     * @param model the model to use to check for sensors
     */

    private void prune(FHEMModel model) {
        Set<String> sensorNames = infoMap.keySet();
        sensorNames.removeIf(s -> !model.getSensorByName(s).isPresent());
    }

    /**
     * Prune the infoMap, removing rules which are not present any more.
     *
     * @param rulesToKeep the new set of rules
     */
    public void prune(Set<Rule> rulesToKeep) {
        Set<String> namesToKeep = rulesToKeep.stream().map(Rule::getName).collect(Collectors.toSet());
        for (Map.Entry<String, Map<Rule, RuleInfo>> stringMapEntry : infoMap.entrySet()) {
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
        model.setHistory(history);
        /* Apply ruleinfos to sensors */
        for (Map.Entry<String, Map<Rule, RuleInfo>> stringMapEntry : infoMap.entrySet()) {
            FHEMSensor sensor = model.getSensorByName(stringMapEntry.getKey())
                    /* Highly unlikely */
                    .orElseThrow(() -> new RuntimeException("Sensor not found in model. " + stringMapEntry.getKey()));
            Set<RuleInfo> shownInApp = stringMapEntry.getValue().keySet().stream().filter(Rule::isVisible)
                    .map(rule -> stringMapEntry.getValue().get(rule)).collect(Collectors.toSet());
            sensor.addRuleInfos(shownInApp);
        }
        /* Generate and add snapshots */
        model.addStateSnapshot(snapshots);
    }

    private void updateSnapshot(Set<Rule> rules) {
        snapshots.clear();
        for (RuleState state : stateMap.values()) {
            if (state.isOk()) {
                continue;
            }
            Rule rule = rules.stream().filter(r -> r.getName().equals(state.getRuleName())).findAny().orElseThrow(() ->
                    new RuntimeException("Rule from statemap not found: " + state.getRuleName())
            );
            RuleSnapshot snapshot = new RuleSnapshot(state, rule);
            snapshots.add(snapshot);
        }
        snapshots.sort(Comparator.comparingInt(RuleSnapshot::getPriority));
    }

    /**
     * Reset the static state.
     * Useful for unit tests.
     */
    void clear() {
        stateMap.clear();
        infoMap.clear();
        history.clear();
        snapshots.clear();
    }
}
