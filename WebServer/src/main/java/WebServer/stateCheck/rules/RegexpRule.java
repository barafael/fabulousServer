package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.stateCheck.rules.parsing.RuleParam;

/**
 * Created by ra on 07.07.17.
 */
public class RegexpRule extends Rule {

    public RegexpRule(RuleParam ruleParam, FHEMModel model) {
        super(ruleParam, model);
    }

    @Override
    public boolean eval() {
        return false;
    }
}
