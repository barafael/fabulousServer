package WebServer.stateCheck.rules.parsing;

import WebServer.stateCheck.rules.PredicateRule;
import WebServer.stateCheck.rules.RegexpRule;
import WebServer.stateCheck.rules.Rule;
import WebServer.stateCheck.rules.ThreshholdRule;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a collection of rule parameters.
 * It should be deserialized from a rules file.
 *
 * @author Rafael on 07.07.17.
 */
public class RuleParamCollection {
    private List<RuleParam> ruleParams = new ArrayList<>();

    /* TODO: Suppress direct creation */
    /* Currently open because testing */
    public RuleParamCollection(RuleParam... ruleParam) {
        ruleParams.addAll(Arrays.asList(ruleParam));
    }

    public static RuleParamCollection fromJson(String json) throws JsonSyntaxException {
        Gson gson = new Gson();
        /* TODO: validate against fields which are null (not set in the input) */
        return gson.fromJson(json, RuleParamCollection.class);
    }

    /**
     * Convert the ist of rule parameters to rules.
     * @return a set of rules generated from the parameters.
     */
    public Set<Rule> toRules() {
        Set<Rule> rules = new HashSet<>(ruleParams.size() + 5);

        /* RuleName -> { Names of Rules which must be true or false (preconditions) } */
        Map<String, Set<String>> requiredTrueRules = new HashMap<>();
        Map<String, Set<String>> requiredFalseRules = new HashMap<>();

        for (RuleParam ruleParam : ruleParams) {
            /* Filter out the name of the rule itself. Otherwise infinite recursion (self pointer in graph)! */
            Set<String> requiredTrue = ruleParam.getRequiredTrueRules().stream().
                    filter(s -> !s.equals(ruleParam.getName())).collect(Collectors.toSet());
            Set<String> requiredFalse = ruleParam.getRequiredFalseRules().stream().
                    filter(s -> !s.equals(ruleParam.getName())).collect(Collectors.toSet());

            requiredTrueRules.put(ruleParam.getName(), requiredTrue);
            requiredFalseRules.put(ruleParam.getName(), requiredFalse);

            RuleType type = ruleParam.getType();
            switch (type) {
                case REGEXP:
                    rules.add(new RegexpRule(ruleParam));
                    break;
                case THRESHHOLD:
                    rules.add(new ThreshholdRule(ruleParam));
                    break;
                case PREDICATE:
                    rules.add(new PredicateRule(ruleParam));
            }
        }

        /* Use the information from required[True|False]Rules to link a rule back to its preconditions */
        for (Rule rule : rules) {
            /* Associate all the rules which must be true or false */
            Set<String> requiredTrueOfThisRule = requiredTrueRules.get(rule.getName());
            Set<String> requiredFalseOfThisRule = requiredFalseRules.get(rule.getName());

            Set<Rule> trueRules = rules.stream().
                    filter(r -> requiredTrueOfThisRule.contains(r.getName())).collect(Collectors.toSet());
            Set<Rule> falseRules = rules.stream().
                    filter(r -> requiredFalseOfThisRule.contains(r.getName())).collect(Collectors.toSet());

            rule.setRequiredTrue(trueRules);
            rule.setRequiredFalse(falseRules);
        }
        return rules;
    }
}
