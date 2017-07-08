package WebServer.stateCheck.rules.parsing;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.stateCheck.rules.PredicateRule;
import WebServer.stateCheck.rules.RegexpRule;
import WebServer.stateCheck.rules.Rule;
import WebServer.stateCheck.rules.ThreshholdRule;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rafael on 07.07.17.
 */
public class RuleParamCollection {
    List<RuleParam> ruleParams = new ArrayList<>();

    public static RuleParamCollection fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, RuleParamCollection.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this, RuleParamCollection.class);
    }

    public List<Rule> toRules(FHEMModel model) {
        List<Rule> rules = new ArrayList<>(ruleParams.size() + 5);
        for (RuleParam ruleParam : ruleParams) {
            RuleType type = ruleParam.getType();
            switch (type) {
                case REGEXP:
                    rules.add(new RegexpRule(ruleParam, model));
                    break;
                case THRESHHOLD:
                    rules.add(new ThreshholdRule(ruleParam, model));
                    break;
                case PREDICATE:
                    rules.add(new PredicateRule(ruleParam, model));
            }
        }
        return rules;
    }
}
