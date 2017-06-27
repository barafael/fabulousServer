package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * @author Rafael
 * @date 20.06.17.
 */
public class ModelTest {
    FHEMModel model;
    @Before
    public void setUp() {

    }
    @Test
    public void testModelPrint() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        model.ifPresent(fhemModel -> {
            Path path = Paths.get("fhemmodel.json");
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(fhemModel.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testModelRoomIterator() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        model.ifPresent(fhemRooms -> {
            for (FHEMRoom room : fhemRooms) {
                System.out.println(room);
            }
        });
    }

    @Test
    public void testModelLogIterator() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        model.ifPresent(fhemRooms -> {
            for (Iterator<FHEMFileLog> it = fhemRooms.eachLog(); it.hasNext(); ) {
                FHEMFileLog log = it.next();
                System.out.println(log);
            }
        });
    }

    @Test
    public void testModelParseTime() {
        Instant now = Instant.now();
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        model.ifPresent(fhemRooms -> System.out.println(Duration.between(now, Instant.now()).toMillis()));
    }

    @Test
    public void testFilelogSerialization() {
        List<String> permissions = Arrays.asList("permission1","23414");
        Optional<String> json = FHEMParser.getInstance().getFHEMModel(permissions);
        if (json.isPresent()) {
            System.out.println(json);
        }
    }

    @Test
    public void serdeRoundtrip() {
        List<String> permissions = Arrays.asList("permission1","23414");
        Optional<String> json = FHEMParser.getInstance().getFHEMModel(permissions);
        json.ifPresent(s -> {
            FHEMModel model2 = new Gson().fromJson(s, FHEMModel.class);
            System.out.println(new Gson().toJson(model2));
        });
    }

    @Test
    public void validJsonWithPermissions() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Collections.singletonList("96_Pwr_Current");
        Optional<String> model_str = parser.getFHEMModel(permissions);
        if (model_str.isPresent()) {
            assert isValidJSON(model_str.get());
        } else assert false;
    }

    public static boolean isValidJSON(String jsonInString) {
        Gson gson = new Gson();
        try {
            gson.fromJson(jsonInString, Object.class);
            return true;
        } catch(com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }
}
