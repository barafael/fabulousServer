package webserver.ruleCheck.parsing;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import webserver.ruleCheck.rules.GeneralPredicate;
import webserver.ruleCheck.rules.NumericRule;
import webserver.ruleCheck.rules.RegexpRule;
import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.SensorPredicate;

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
public final class RuleParamCollection {
    /**
     * A set of parameters from which to construct rules.
     */
    @SerializedName("Rules")
    private final Set<RuleParam> ruleParams = new HashSet<>();

    /** TODO: Suppress direct creation
     * Currently open for unit tests
     * Constructs a param collection.
     * @param ruleParam a varargs list of rule parameters
     */
    public RuleParamCollection(RuleParam... ruleParam) {
        ruleParams.addAll(Arrays.asList(ruleParam));
    }

    /**
     * Construct an instance of this class directly from the given JSON.
     * @param json a string containing a valid rule definition
     * @return a set of rule parameters
     * @throws JsonSyntaxException if the input string was not valid JSON
     */
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
                case NUMERIC:
                    rules.add(new NumericRule(ruleParam));
                    break;
                case GENERAL_PRED:
                    rules.add(new GeneralPredicate(ruleParam));
                    break;
                case SENSOR_PRED:
                    rules.add(new SensorPredicate(ruleParam));
                    break;
                case UNKNOWN:
                    System.err.println("Unimplemented Rule Type! Expression was: '" + ruleParam.getExpression() + "'. "
                            + "Maybe you need to add arguments?");
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
