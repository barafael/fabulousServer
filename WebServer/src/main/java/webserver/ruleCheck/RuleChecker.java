package webserver.ruleCheck;

import com.google.gson.JsonSyntaxException;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.parsing.RuleParamCollection;
import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleInfo;
import webserver.ruleCheck.rules.RuleState;

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
public final class RuleChecker {
    private static RuleChecker instance;
    private final State fhemState = new State();

    private RuleChecker() {
    }

    /**
     * Acquisitor for RuleChecker instance.
     *
     * @return the instance of this singleton
     */
    public static synchronized RuleChecker getInstance() {
        if (RuleChecker.instance == null) {
            RuleChecker.instance = new RuleChecker();
        }
        return RuleChecker.instance;
    }

    /**
     * Get rule parameters from a file and parse them to rules.
     *
     * @param path path to the rules file
     * @return a set of parsed rules
     */
    private Optional<Set<Rule>> getRules(String path) {
        RuleParamCollection params;
        try {
            params = loadRuleParams(path);
        } catch (IOException e) {
            System.err.println("The file " + path + " could not be read because there was an IO exception.");
            return Optional.empty();
        } catch (JsonSyntaxException e) {
            System.err.println("There seems to be a syntax error in rules.json.");
            return Optional.empty();
        }
        return Optional.of(params.toRules());
    }

    public void evaluate(FHEMModel model) {
        evaluate(model, "rules.json");
    }

    public void evaluate(FHEMModel model, String path) {
        Optional<Set<Rule>> rules_opt = getRules(path);
        rules_opt.ifPresent(rules -> evaluate(model, rules));
    }

    private void evaluate(FHEMModel model, Set<Rule> rules) {
        Set<RuleState> states = rules.stream().map(rule -> rule.eval(model)).collect(Collectors.toSet());
        fhemState.update(states);
        fhemState.apply(model);
        //fhemState.report(model);
   //    for (Rule rule : rules) {
   //        /* Rulestate contains information about all passed and violated sensors */
   //        RuleState ruleState = rule.eval(model);

   //        /* Add ok ruleinfo to the sensors where this rule was alright */
   //        for (FHEMSensor sensor : ruleState.getOkSensors()) {
   //            sensor.addPassedRule(
   //                    new RuleInfo(
   //                            rule.getName(),
   //                            true,
   //                            Instant.now().getEpochSecond(),
   //                            rule.getPermissionField(),
   //                            rule.getOkMessage()));
   //        }

   //        Long now = Instant.now().getEpochSecond();
   //        /* Get all names of sensors which are ok */
   //        Set<String> okSensorNames = ruleState.getOkSensors().stream()
   //                .map(FHEMSensor::getName).collect(Collectors.toSet());
   //        for (String sensorName : okSensorNames) {
   //            /* Remove passed rules from violated rules of this sensor */
   //            Map<String, Long> violatedRules = fhemState.violatedRules.get(sensorName);
   //            if (violatedRules != null) {
   //                violatedRules.keySet().removeIf(s -> s.equals(rule.getName()));
   //            } /* else, sensor did not violate any rules (then get() resulted in null) */

   //            /* If no rules at all are violated now, remove the sensor from the state */
   //            if (violatedRules == null || violatedRules.isEmpty()) {
   //                fhemState.violatedRules.remove(sensorName);
   //            }
   //        }

   //        /* Get all names of sensors which are not ok */
   //        Set<String> violatedSensorNames = ruleState.getViolatedSensors().stream().
   //                map(FHEMSensor::getName).collect(Collectors.toSet());
   //        for (String sensorName : violatedSensorNames) {
   //            if (fhemState.violatedRules.containsKey(sensorName)) {
   //                /* Get a map of violated rules and their timestamps */
   //                Map<String, Long> violatedRules = fhemState.violatedRules.get(sensorName);
   //                if (!violatedRules.containsKey(rule.getName())) {
   //                    /* Rule was not yet violated */
   //                    violatedRules.put(rule.getName(), now);
   //                }
   //            } else {
   //                Map<String, Long> violatedRules = new HashMap<>();
   //                violatedRules.put(rule.getName(), now);
   //                fhemState.violatedRules.put(sensorName, violatedRules);
   //            }
   //       }
   //   }
   //   fhemState.setRuleInfos(model, rules, null);
    }

    private RuleParamCollection loadRuleParams(String path) throws IOException, JsonSyntaxException {
        /* TODO add translations for rules? */
        String content = new String(Files.readAllBytes(Paths.get(path)));
        return RuleParamCollection.fromJson(content);
    }

    public void clear() {
        fhemState.clear();
    }
}
