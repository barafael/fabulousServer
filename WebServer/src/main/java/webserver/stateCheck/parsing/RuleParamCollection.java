package webserver.stateCheck.parsing;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import webserver.stateCheck.rules.PredicateRule;
import webserver.stateCheck.rules.RegexpRule;
import webserver.stateCheck.rules.Rule;
import webserver.stateCheck.rules.NumericRule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represents a collection of rule parameters.
 * It should be deserialized from a rules file.
 *
 * @author Rafael on 07.07.17.
 */
public class RuleParamCollection {
    private Set<RuleParam> ruleParams = new HashSet<>();

    /* TODO: Suppress direct creation */
    /* Currently open because testing */
    public RuleParamCollection(RuleParam... ruleParam) {
        ruleParams.addAll(Arrays.asList(ruleParam));
    }

    public static RuleParamCollection fromJson(String json) throws JsonSyntaxException {
        Gson gson = new Gson();
        RuleParamCollection ruleParamCollection = gson.fromJson(json, RuleParamCollection.class);
        /* Gson incorrectly deserializes incorrect json with too many commas.
           This is why null values have to be cleaned up here.
        */
        ruleParamCollection.ruleParams.removeIf(Objects::isNull);

        return ruleParamCollection;
    }

    /**
     * Keep for now in case I want to change equals() method
     */
    private void filterDuplicates() {
        Set<String> rulenames = ruleParams.stream().map(RuleParam::getName).collect(Collectors.toSet());
        if (rulenames.size() != ruleParams.size()) {
            System.err.println("The rules definition contained duplicate entries (entries with duplicate names)!");
        } else {
            return;
        }
        Set<RuleParam> dedup = new HashSet<>();
        for (String rulename : rulenames) {
            dedup.add(ruleParams.stream().filter(ruleParam -> ruleParam.getName().equals(rulename)).findAny().get());
        }
        ruleParams = dedup;
    }

    /**
     * Convert the ist of rule parameters to rules.
     *
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

            if (requiredTrue.size() != ruleParam.getRequiredTrueRules().size() ||
                    requiredFalse.size() != ruleParam.getRequiredFalseRules().size()) {
                System.err.println("Ignoring rule self dependency!");
                System.err.println("Rule Parameters: " + ruleParam);
            }

            requiredTrueRules.put(ruleParam.getName(), requiredTrue);
            requiredFalseRules.put(ruleParam.getName(), requiredFalse);

            RuleType type = ruleParam.getType();
            switch (type) {
                case REGEXP:
                    rules.add(new RegexpRule(ruleParam));
                    break;
                case THRESHHOLD:
                    rules.add(new NumericRule(ruleParam));
                    break;
                case PREDICATE:
                    rules.add(new PredicateRule(ruleParam));
                    break;
                default:
                    System.err.println("Unimplemented Rule Type!");
            }
        }

        /* Use the information from required[True|False]Rules to link a rule back to its preconditions */
        for (Rule rule : rules) {
            /* Associate all the rules which must be true or false */
            Set<String> requiredTrueOfThisRule = requiredTrueRules.get(rule.getName());
            Set<String> requiredFalseOfThisRule = requiredFalseRules.get(rule.getName());

            Set<Rule> trueRules = rules.stream().
                    filter((Rule r) -> requiredTrueOfThisRule.contains(r.getName())).collect(Collectors.toSet());
            Set<Rule> falseRules = rules.stream().
                    filter(r -> requiredFalseOfThisRule.contains(r.getName())).collect(Collectors.toSet());

            rule.setRequiredTrue(trueRules);
            rule.setRequiredFalse(falseRules);
        }
        return rules;
    }

    @Override
    public int hashCode() {
        return ruleParams != null ? ruleParams.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleParamCollection that = (RuleParamCollection) o;

        return ruleParams != null ? ruleParams.equals(that.ruleParams) : that.ruleParams == null;
    }

    @Override
    public String toString() {
        return "RuleParamCollection{"
                + "ruleParams=" + ruleParams
                + '}';
    }
}
