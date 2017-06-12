package fhemModel.sensors;
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
}
