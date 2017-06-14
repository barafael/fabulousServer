package fhemModel.sensors;

import fhemUtils.FHEMUtils;

/**
 * This class represents a room which associates a FHEM model.
 * @author Rafael
 */

public class Room {
    private SVGRoomPlan plan;
    private String name;

    public Room(SVGRoomPlan plan, String name) {
        this.plan = plan;
        this.name = name;
    }

    public Room(String roomname) {
        String pathToPlan = FHEMUtils.getFHEMDIR() + "roomplans/" + roomname + ".svg";
        plan = SVGRoomPlan.loadFile(pathToPlan);
        name = roomname;
    }

    public boolean isAppRoom() {
        return name.startsWith("room_");
    }

    @Override
    public String toString() {
        return "Room{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Room room = (Room) o;

        return name.equals(room.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
