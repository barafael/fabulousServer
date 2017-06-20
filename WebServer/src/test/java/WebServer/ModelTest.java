package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Optional;

/**
 * Created by ra on 20.06.17.
 */
public class ModelTest {
    @Test
    public void testModelPrint() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        try {
            PrintWriter pw = new PrintWriter("fhemmodel.json");
            model.ifPresent(pw::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
