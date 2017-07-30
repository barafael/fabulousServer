package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.ruleCheck.parsing.RuleParam;

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
        if (expression != null && !expression.isEmpty()) {
            System.err.println("Something went wrong: expression was not empty. Ignoring expression.");
            System.err.println("(Expressions make no sense for metarules, which only depend on other rules).");
        }

        isEvaluated = true;
        ruleState = new RuleState(true, this);
        return ruleState;
    }
}
