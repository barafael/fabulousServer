package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.stateCheck.WARNINGLEVEL;
import WebServer.stateCheck.rules.parsing.RuleParam;

import java.time.Instant;
import java.util.*;

/**
 * This abstract class contains the attributes of a rule in FHEM, most notably the eval(model) method.
 *  @author Rafael
 *
 */

public abstract class Rule {
    String name;
    String permission;
    Set<FHEMSensor> sensorNames;
    Set<FHEMSensor> affectedSensorNames;
    Set<String> requiredTrueRules;
    Set<String> requiredFalseRules;
    String okMessage;
    WARNINGLEVEL level;
    Map<WARNINGLEVEL, String> errorMessages;
    Map<Long, WARNINGLEVEL> escalation = new TreeMap<>();
    long lastChangeTimestamp;
    final boolean state;
    boolean lastState;

    FHEMModel model;

    public Rule(RuleParam ruleParam, FHEMModel model) {
        name = ruleParam.getName();
        permission = ruleParam.getPermissionField();
        sensorNames = model.getSensorsByCollection(ruleParam.getSensorNames());
        affectedSensorNames = new HashSet<>();
        okMessage = ruleParam.getOkMessage();
        requiredTrueRules = ruleParam.getRequiredTrueRules();
        requiredFalseRules = ruleParam.getRequiredFalseRules();
        level = WARNINGLEVEL.NORMAL;
        errorMessages = ruleParam.getErrorMessages;
        lastChangeTimestamp = Instant.now().getEpochSecond();
        state = true;
        lastState = true;

        this.model = model;
    }

    public abstract boolean eval();

    public String log() {
        StringBuilder out = new StringBuilder();
        out.append(name).append('\n');
        if (state) {
            out.append(okMessage).append('\n');
        } else {
            out.append(errorMessages.get(level)).append('\n');
        }
        out.append("Affected sensors: \n\t").append(affectedSensorNames).append("\n\t");
        return out.toString();
    }

    Rule stamp() {
        lastChangeTimestamp = Instant.now().getEpochSecond();
        return this;
    }

    void newState(boolean state) {
        if (state == lastState) {
            if (!state) {
                long time_passed = Instant.now().getEpochSecond() - lastChangeTimestamp;
                setLevel(time_passed);
            }
        } else {
            lastState = state;
            stamp();
        }
    }

    private void setLevel(long time_passed) {
        for (long time: escalation.keySet()) {
            if (time_passed > time) {
                level = escalation.get(time);
                return;
            }
        }
    }
}
