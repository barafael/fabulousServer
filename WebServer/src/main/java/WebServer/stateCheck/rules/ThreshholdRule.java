package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.stateCheck.rules.parsing.RuleParam;

/**
 * @author Rafael on 07.07.17.
 */
public class ThreshholdRule extends Rule {
    public ThreshholdRule(RuleParam ruleParam) {
        super(ruleParam);
    }

    @Override
    public RuleState realEval(FHEMModel model) {
        return null;
    }
}
