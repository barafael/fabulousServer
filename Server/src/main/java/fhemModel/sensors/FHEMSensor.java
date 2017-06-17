package fhemModel.sensors;

import fhemModel.timeserie.FHEMFileLog;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class represents the data about a sensor gathered from FHEM.
 *
 * @author Rafael
 */

public class FHEMSensor {
    private Coordinates coord;
    private final String name;
    private final long ID;
    private String permission;
    private final HashSet<FHEMRoom> rooms = new HashSet<>();
    private HashSet<FHEMFileLog> associatedLogs = new HashSet<>();
    private boolean isShowInApp;
    private HashMap<String, String> metaInfo;

    public FHEMSensor(int coordX, int coordY, String name, long ID, String permission,
                      boolean isShowInApp, HashMap<String, String> metaInfo, Collection<FHEMRoom> rooms) {
        this.coord = new Coordinates(coordX, coordY);
        this.name = name;
        this.ID = ID;
        this.permission = permission;
        this.isShowInApp = isShowInApp;
        this.metaInfo = metaInfo;
        addRooms(rooms);
    }

    public void addMeta(@NotNull String key, @NotNull String value) {
        metaInfo.put(key, value);
    }

    public void addLog(FHEMFileLog log) {
        associatedLogs.add(log);
    }

    public boolean addRooms(Collection<FHEMRoom> r) {
        return rooms.addAll(r);
    }

    public String getName() {
        return name;
    }
}
