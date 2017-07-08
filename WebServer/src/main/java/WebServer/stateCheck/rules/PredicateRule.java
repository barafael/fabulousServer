package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.stateCheck.rules.parsing.RuleParam;

/**
 * @author Rafael on 07.07.17.
 */
public class PredicateRule extends Rule {
    public PredicateRule(RuleParam ruleParam, FHEMModel model) {
        super(ruleParam, model);
    }

    @Override
    public boolean eval() {
        return false;
    }
}
