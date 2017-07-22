package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.ruleCheck.PredicateCollection;
import webserver.ruleCheck.parsing.RuleParam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author Rafael
 */

public class Meta extends Rule {
    /**
     * Construct a general predicate.
     *
     * @param ruleParam the parameters of the general predicate
     */
    public Meta(RuleParam ruleParam) {
        super(ruleParam);
    }

    /**
     * Specific evaluation of a general predicate on a model.
     *
     * @param model the model to use information from
     * @return the rule state, containing violated and passed sensors
     */
    @Override
    public RuleState specificEval(FHEMModel model) {
        /*
        Input validation
        */
        if (expression != null || !expression.isEmpty()) {
            System.err.println("Something went wrong: expression contained stuff.");
           //TODO: metarule if getType switch fallthrough? (to ignore specificeval and only care about eval)
        }

        isEvaluated = true;
        ruleState = new RuleState(true, this);
        return ruleState;
    }
}
