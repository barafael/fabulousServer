package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
}
