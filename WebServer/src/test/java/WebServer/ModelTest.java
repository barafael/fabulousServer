package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.sensors.FHEMRoom;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;

/**
 * @author Rafael
 * @date 20.06.17.
 */
public class ModelTest {
    @Test
    public void testModelPrint() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        if (model.isPresent()) {
            Path path = Paths.get("fhemmodel.json");
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(model.get().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testModelRoomIterator() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        if (model.isPresent()) {
            for (FHEMRoom room : model.get()) {
                System.out.println(room);
            }
        }
    }

    @Test
    public void testModelLogIterator() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        if (model.isPresent()) {
            for (Iterator<FHEMFileLog> it = model.get().eachLog(); it.hasNext(); ) {
                FHEMFileLog log = it.next();
                System.out.println(log);
            }
        }
    }
}
