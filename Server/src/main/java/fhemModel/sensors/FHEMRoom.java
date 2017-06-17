package fhemModel.sensors;

import fhemUtils.FHEMUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a room which associates a FHEM model.
 * @author Rafael
 */

public class FHEMRoom {
    public List<FHEMSensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<FHEMSensor> sensors) {
        this.sensors = sensors;
    }

    List<FHEMSensor> sensors = new ArrayList<>();
    private SVGRoomPlan plan;
    private String name;

    public FHEMRoom(String roomname) {
        String pathToPlan = FHEMUtils.getFHEMDIR() + "roomplans/" + roomname + ".svg";
        plan = SVGRoomPlan.loadFile(pathToPlan);
        name = roomname;
    }

    public boolean isAppRoom() {
        return name.startsWith("room_");
    }

    @Override
    public String toString() {
        return "FHEMRoom{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FHEMRoom room = (FHEMRoom) o;

        return name.equals(room.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
