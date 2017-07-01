package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.stateCheck.State;
import WebServer.stateCheck.WARNINGLEVEL;

import java.time.Instant;
import java.util.*;

/**
 * This abstract class contains the attributes of a rule in fhem, most notably the eval(model) method.
 *  @author Rafael
 *
 */

public abstract class Rule {
    String name;
    String permission;
    final Set<String> sensors;
    final Set<String> affectedSensors;
    String okMessage;
    private WARNINGLEVEL level;
    final Map<WARNINGLEVEL, String> errorMessages;
    private final Map<Long, WARNINGLEVEL> escalation = new TreeMap<>();
    private long lastChangeTimestamp;
    private final transient boolean state;
    private transient boolean lastState;

    Rule() {
        name = "";
        permission = "";
        sensors = new HashSet<>();
        affectedSensors = new HashSet<>();
        okMessage = "";
        level = WARNINGLEVEL.NORMAL;
        errorMessages = new HashMap<>();
        lastChangeTimestamp = Instant.now().getEpochSecond();
        state = true;
        lastState = true;
    }


    public abstract boolean eval(FHEMModel model, State state);

    public String log() {
        StringBuilder out = new StringBuilder();
        out.append(name).append('\n');
        if (state) {
            out.append(okMessage).append('\n');
        } else {
            out.append(errorMessages.get(level)).append('\n');
        }
        out.append("Affected sensors: ").append(sensors).append('\n');
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
