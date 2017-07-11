package webserver.stateCheck;

import com.google.gson.JsonSyntaxException;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.stateCheck.rules.Rule;
import webserver.stateCheck.rules.RuleState;
import webserver.stateCheck.parsing.RuleParamCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is a singleton because the state should not be overwritten.
 * The state stores sensors which violate one or more rules, and
 * contains the time at which a sensor violated a specific condition.
 *
 * @author Rafael
 */

public class StateChecker {
    private static StateChecker instance;
    /* (Sensorname -> (Rulename -> timeAtViolation)) */
    /* If a rule is violated, it appears in the hashmap with its start time */
    private final State fhemState = new State();

    private StateChecker() {
    }

    /**
     * Aquisitor for StateChecker instance
     *
     * @return the instance of this singleton
     */
    public static synchronized StateChecker getInstance() {
        if (StateChecker.instance == null) {
            StateChecker.instance = new StateChecker();
        }
        return StateChecker.instance;
    }

    private RuleParamCollection loadRuleParams(String path) throws IOException, JsonSyntaxException {
        /* TODO add translations for rules? */
        String content = new String(Files.readAllBytes(Paths.get(path)));
        return RuleParamCollection.fromJson(content);
    }

    private Optional<Set<Rule>> getRules(String path) {
        RuleParamCollection params;
        try {
            params = loadRuleParams(path);
        } catch (IOException e) {
            System.err.println("The file 'rules.json' could not be read because there was an IO exception.");
            return Optional.empty();
        } catch (JsonSyntaxException e) {
            System.err.println("There seems to be a syntax error in rules.json.");
            return Optional.empty();
        }
        return Optional.of(params.toRules());
    }


    public boolean evaluate(FHEMModel model) {
        return evaluate(model, "rules.json");
    }

    public boolean evaluate(FHEMModel model, String path) {
        Optional<Set<Rule>> rules_opt = getRules(path);
        if (!rules_opt.isPresent()) {
            return false;
        }
        Set<Rule> rules = rules_opt.get();

        for (Rule rule : rules) {
            RuleState ruleState = rule.eval(model);

            Long now = Instant.EPOCH.getEpochSecond();

            if (ruleState.isOk()) {
                /* Consistency check */
                if (!ruleState.getViolatedSensors().isEmpty()) {
                    System.err.println("Error! Rule is ok but violatedSensors is not empty!");
                    /* TODO: Mark some violated sensors? */
                }
                /* Get all names of sensors which are ok */
                Set<String> sensorNames = ruleState.getOkSensors().stream().
                        map(FHEMSensor::getName).collect(Collectors.toSet());
                for (String sensorName : sensorNames) {
                    /* Remove ok rules from violated rules of this sensor */
                    Map<String, Long> violatedRules = fhemState.state.get(sensorName);
                    if (violatedRules != null) {
                        violatedRules.keySet().removeIf(s -> s.equals(rule.getName()));
                    } /* else, sensor did not violate any rules (then get resulted in null) */
                    /* If no rules at all are violated now, remove the sensor from the state */
                    if (violatedRules == null || violatedRules.isEmpty()) {
                        fhemState.state.remove(sensorName);
                    }
                }
            } else {
                /* ruleState is not ok */
                /* Get all names of sensors which are ok */
                Set<String> okSensorNames = ruleState.getOkSensors().stream().
                        map(FHEMSensor::getName).collect(Collectors.toSet());
                for (String sensorName : okSensorNames) {
                    /* Remove ok rules from violated rules of this sensor */
                    Map<String, Long> violatedRules = fhemState.state.get(sensorName);
                    if (violatedRules != null) {
                        violatedRules.keySet().removeIf(s -> s.equals(rule.getName()));
                    } /* else, sensor did not violate any rules (then get resulted in null) */

                    /* If no rules at all are violated now, remove the sensor from the state */
                    if (violatedRules == null || violatedRules.isEmpty()) {
                        fhemState.state.remove(sensorName);
                    }
                }

                /* Get all names of sensors which are not ok */
                Set<String> violatedSensorNames = ruleState.getViolatedSensors().stream().
                        map(FHEMSensor::getName).collect(Collectors.toSet());
                for (String sensorName : violatedSensorNames) {
                    if (fhemState.state.containsKey(sensorName)) {
                        /* Get a map of violated rules and their timestamps */
                        Map<String, Long> violatedRules = fhemState.state.get(sensorName);
                        if (!violatedRules.containsKey(rule.getName())) {
                            /* Rule was not yet violated */
                            violatedRules.put(rule.getName(), now);
                        }
                    } else {
                        Map<String, Long> violatedRules = new HashMap<>();
                        violatedRules.put(rule.getName(), now);
                        fhemState.state.put(sensorName, violatedRules);
                    }
                }
            }
        }
        fhemState.setRuleInfos(model, rules);
        return true;
    }
}
