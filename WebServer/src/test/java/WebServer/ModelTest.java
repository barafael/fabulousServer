package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import com.google.gson.Gson;
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
    @Test
    public void modelPrint() {
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
    public void modelRoomIterator() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        model.ifPresent(fhemRooms -> {
            int count = 0;
            for (FHEMRoom room : fhemRooms) {
                count++;
            }
            assert count > 0;
            System.out.println(count + " rooms.");
        });
    }

    @Test
    public void modelLogIterator() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        model.ifPresent(fhemRooms -> {
            int count = 0;
            for (Iterator<FHEMFileLog> it = fhemRooms.eachLog(); it.hasNext(); ) {
                FHEMFileLog log = it.next();
                assert log != null;
                count++;
            }
            assert count > 0;
            System.out.println(count + " logs.");
        });
    }

    @Test
    public void modelParseTimeWithoutPermissions() {
        Instant now = Instant.now();
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        model.ifPresent(fhemRooms -> System.out.println("Without permissions: " + Duration.between(now, Instant.now()).toMillis()));
    }

    @Test
    public void modelParseTimeWithPermissions() {
        Instant now = Instant.now();
        Optional<String> model = FHEMParser.getInstance().getFHEMModelJSON(Arrays.asList("permission1", "S_Fenster"));
        model.ifPresent(fhemRooms -> System.out.println("With permissions: " + Duration.between(now, Instant.now()).toMillis()));
    }

    @Test
    public void parseWithoutPermissionsJSON() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        assert isValidJSON(model.get().toJson());
    }

    @Test
    public void modelWithoutPermissionsToString() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        assert isValidJSON(model.get().toString());
    }
    @Test
    public void toJSONWithPermissions() {
        List<String> permissions = Arrays.asList("Permission1", "S_Fenster");
        Optional<String> json = FHEMParser.getInstance().getFHEMModelJSON(permissions);
        assert json.isPresent();
        assert isValidJSON(json.get());
    }

    @Test
    public void toJSONWithEmptyStringPermissions() {
        List<String> permissions = Collections.singletonList("");
        Optional<String> json = FHEMParser.getInstance().getFHEMModelJSON(permissions);
        assert json.isPresent();
        assert isValidJSON(json.get());
    }

    @Test
    public void serDeRoundtrip() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        String json1 = model.get().toJson();
        assert isValidJSON(json1);
        FHEMModel model2 = new Gson().fromJson(json1, FHEMModel.class);
        String json2 = model2.toJson();
        assert json1.length() == json2.length();
    }

    @Test
    public void toJSONWithPermissionsNoMatch() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Arrays.asList("nomatch", "othernomatch");
        Optional<String> json = parser.getFHEMModelJSON(permissions);
        assert json.isPresent();
        assert isValidJSON(json.get());
        /* There was no match! */
        assert json.get().equals("null");
    }

    @Test
    public void getRoomNoHash() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Arrays.asList("testing", "otherperm");
        Optional<String> room = parser.getRoomplan("room_testing", permissions);
        assert room.isPresent();
        assert room.get().equals("this is no moon.\n");
    }

    @Test
    public void getRoomWithHash() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Arrays.asList("testing", "otherperm");
        String content = "this is no moon.\n";
        int hash = content.hashCode();
        parser.getFHEMModel();
        parser.setRoomplan("room_testing", "this is no moon.\n");
        Optional<String> roomplan = parser.getRoomplan("room_testing", hash, permissions);
        /* Because hashes were equal! */
        assert !roomplan.isPresent();
    }

    @Test
    public void getRoomWithoutHash() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Arrays.asList("testing", "otherperm");
        String content = "this is no moon.\n";
        int hash = content.hashCode() + 1;
        parser.getFHEMModel();
        parser.setRoomplan("room_testing", "this is no moon.\n");
        Optional<String> roomplan = parser.getRoomplan("room_testing", hash, permissions);
        assert roomplan.isPresent();
        assert roomplan.get().equals("this is no moon.\n");
        System.out.println(roomplan.get());
    }

    /**
     * Helper function which tests if a json string can be deserialized without throwing an exception.
     * @param json A string which should be tested
     * @return whether the input was valid json
     */
    private static boolean isValidJSON(String json) {
        Gson gson = new Gson();
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch(com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }
}
