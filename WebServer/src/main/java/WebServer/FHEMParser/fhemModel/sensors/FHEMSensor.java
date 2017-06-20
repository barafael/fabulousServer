package WebServer.FHEMParser.fhemModel.sensors;

import WebServer.FHEMParser.fhemModel.timeserie.FHEMFileLog;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

/**
 * This class represents the data about a sensor gathered from FHEM.
 *
 * @author Rafael
 */

public class FHEMSensor {
    private final Coordinates coord;
    private final String name;
    private final long ID;
    private final String permission;
    private final HashSet<FHEMFileLog> fileLogs = new HashSet<>();
    private final boolean isShowInApp;
    private final HashMap<String, String> metaInfo;
    private String icon;

    public FHEMSensor(int coordX, int coordY, String name, long ID, String permission,
                      boolean isShowInApp, HashMap<String, String> metaInfo) {
        this.coord = new Coordinates(coordX, coordY);
        this.name = name;
        this.ID = ID;
        this.permission = permission;
        this.isShowInApp = isShowInApp;
        this.metaInfo = metaInfo;
    }

    public void addMeta(@NotNull String key, @NotNull String value) {
        metaInfo.put(key, value);
    }

    public void addLog(FHEMFileLog log) {
        fileLogs.add(log);
    }

    public String getName() {
        return name;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public Optional<FHEMFileLog> getLogByName(String filelogName) {
        for (FHEMFileLog log : fileLogs) {
            if (log.getName().equals(filelogName)) {
                return Optional.of(log);
            }
        }
        return Optional.empty();
    }
}
