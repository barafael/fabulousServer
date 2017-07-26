package webserver.ruleCheck;

import com.google.gson.JsonSyntaxException;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.ruleCheck.parsing.RuleParamCollection;
import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    /**
     * The singleton instance.
     */
    private static RuleChecker instance;
    /**
     * The static state of FHEM, consisting of passed and violated rules and their history.
     */
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

    /**
     * Evaluate a model with the default rule set.
     *
     * @param model the model to acquire the information from
     */
    public void evaluate(FHEMModel model) {
        evaluate(model, "rules.json");
    }

    /**
     * Evaluate a model, given the path to a rules file.
     *
     * @param model the model to evaluate
     * @param path  the path to the set of rules
     */
    public void evaluate(FHEMModel model, String path) {
        Optional<Set<Rule>> rules_opt = getRules(path);
        rules_opt.ifPresent(rules -> evaluate(model, rules));
    }

    /**
     * Evaluate a model given a set of rules.
     *
     * @param model the model to evaluate
     * @param rules the set of rules
     */
    private void evaluate(FHEMModel model, Set<Rule> rules) {
        Set<RuleState> states = rules.stream().map(rule -> rule.eval(model)).collect(Collectors.toSet());
        //fhemState.prune(rules);
        fhemState.update(states, rules);
        fhemState.apply(model);
    }

    /**
     * Load rule parameters from a given file.
     *
     * @param path a string containing a path to the rules file
     * @return a collection of rule parameters
     *
     * @throws IOException         if reading the rules file went wrong
     * @throws JsonSyntaxException if the syntax of the rules file was incorrect json
     */
    private RuleParamCollection loadRuleParams(String path) throws IOException, JsonSyntaxException {
        /* TODO add translations for rules? */
        String content = new String(Files.readAllBytes(Paths.get(path)));
        return RuleParamCollection.fromJson(content);
    }

    /**
     * Completely reset the FHEM state.
     * Useful for unit tests.
     */
    public void clear() {
        fhemState.clear();
    }
}
