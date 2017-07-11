package webserver.stateCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.stateCheck.parsing.RuleParam;

/**
 * @author Rafael on 07.07.17.
 *         TODO implementation
 */
public class PredicateRule extends Rule {
    public PredicateRule(RuleParam ruleParam) {
        super(ruleParam);
    }

    @Override
    public RuleState realEval(FHEMModel model) {
        return null;
    }
}
