package WebServer.FHEMParser.FHEMRules;

import com.google.gson.Gson;

import java.time.Instant;

/**
 * @author Rafael
 */

public class FHEMRule {
    private String name;
    private String expression;
    private String permission;
    private String errorMessage;
    private String okMessage;
    private long lastChangeTimestamp;
    transient private boolean state;
    transient private boolean lastState;

    /* Prevent init (must happen over parseFromJson() */
    private FHEMRule () {}

    public FHEMRule parseFromJson(String json) {
        return new Gson().fromJson(json, FHEMRule.class).stamp();
    }

    private FHEMRule stamp() {
        lastChangeTimestamp = Instant.now().getEpochSecond();
        return this;
    }

    public boolean eval(FHEMState state) {
        return false;
    }

    public void log() {

    }
}
