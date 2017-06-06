package FHEMModel.sensors;

import FHEMModel.timeserie.Timeserie;
import parser.fhemJson.FHEMDevice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

/**
 * This class represents the data about a sensor gathered from FHEM.
 *
 * @author Rafael
 */

public class Sensor {
    private Coordinates coord;
    private final String name;
    private final long ID;
    private String permission;
    private final HashSet<Room> rooms = new HashSet<>();
    private String status;
    private HashSet<Timeserie> associatedSeries = new HashSet<>();
    private boolean isShowInApp;
    private HashMap<String, String> metaInfo;

    public Sensor(int coordX, int coordY, String name, long ID, String permission, String status,
                  boolean isShowInApp, HashMap<String, String> metaInfo) {
        this.coord = new Coordinates(coordX, coordY);
        this.name = name;
        this.ID = ID;
        this.permission = permission;
        this.status = status;
        this.isShowInApp = isShowInApp;
        this.metaInfo = metaInfo;
    }

    public void associateSeries(HashSet<Timeserie> associatedSeries) {
        this.associatedSeries.addAll(associatedSeries);
    }

    public boolean addRoom(Room r) {
        return rooms.add(r);
    }
}
