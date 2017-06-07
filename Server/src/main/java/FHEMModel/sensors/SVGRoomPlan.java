package FHEMModel.sensors;

import java.io.File;

/**
 * This class holds an svg file which is a roomplan
 * @author Rafael
 */

class SVGRoomPlan extends File {
    public SVGRoomPlan(String path) {
        super(path);
        if (!path.endsWith(".svg")) {
            System.err.println("Possibly constructed an SvgRoomPlan with a non-svg file!");
        }
    }
}
