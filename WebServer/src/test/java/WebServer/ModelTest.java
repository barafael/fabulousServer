package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemConnection.FHEMClientModeCon;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.log.Timeserie;
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
    private static final int DRÖLF = 42;

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
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();
        int count = 0;
        for (FHEMRoom room : model) {
            assert room != null;
            count++;
        }
        assert count > 0;
        System.out.println(count + " rooms.");
    }

    //TODO annotate sensors in fhem
    @Test
    public void modelSensorIterator() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();
        int count = 0;
        for (Iterator<FHEMSensor> it = model.eachSensor(); it.hasNext(); ) {
            FHEMSensor sensor = it.next();
            assert sensor != null;
            count++;
        }
        assert count > 0;
        System.out.println(count + " sensors.");
    }

    @Test
    public void modelLogIterator() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();
        int count = 0;
        for (Iterator<FHEMFileLog> it = model.eachLog(); it.hasNext(); ) {
            FHEMFileLog log = it.next();
            assert log != null;
            count++;
        }
        assert count > 0;
        System.out.println(count + " logs.");
    }

    @Test
    public void modelParseTimeWithoutPermissions() {
        Instant now = Instant.now();
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        model.ifPresent(fhemRooms -> System.out.println(
                "Without permissions: " + Duration.between(now, Instant.now()).toMillis()));
    }

    @Test
    public void modelParseTimeWithPermissions() {
        Instant now = Instant.now();
        Optional<String> model = FHEMParser.getInstance().getFHEMModelJSON(Arrays.asList(
                "permission1", "S_Fenster"));
        model.ifPresent(fhemRooms -> System.out.println(
                "With permissions: " + Duration.between(now, Instant.now()).toMillis()));
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
        assert room.get().equals("that's no moon.\n");
    }

    @Test
    public void getRoomWithEqualHash() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Arrays.asList("testing", "otherperm");
        String content = "that's no moon.\n";
        int hash = content.hashCode();
        parser.getFHEMModel();
        parser.setRoomplan("room_testing", "that's no moon.\n");
        Optional<String> roomplan = parser.getRoomplan("room_testing", hash, permissions);
        /* Because hashes were equal! */
        assert !roomplan.isPresent();
    }

    @Test
    public void getRoomWithUnequalHash() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Arrays.asList("testing", "otherperm");
        String content = "that's no moon.\n";
        parser.getFHEMModel();
        parser.setRoomplan("room_testing", content);
        Optional<String> roomplan = parser.getRoomplan("room_testing", DRÖLF, permissions);
        assert roomplan.isPresent();
        assert roomplan.get().equals(content);
    }

    @Test
    public void setRoom() {
        FHEMParser parser = FHEMParser.getInstance();
        String content = "...und noch viel weiter\n";
        parser.getFHEMModel();
        parser.setRoomplan("room_testing", content);
        Optional<String> roomplan =
                parser.getRoomplan("room_testing", Collections.singletonList("testing"));
        assert roomplan.isPresent();
        assert roomplan.get().equals(content);
        parser.setRoomplan("room_testing", "that's no moon.\n");
    }

    @Test
    public void testCommandExec() {
        FHEMParser parser = FHEMParser.getInstance();
        System.out.println(parser.execCommand("uname -a"));
    }

    //@Test
    public void testSetSensorPosition() {
        FHEMParser parser = FHEMParser.getInstance()
                .setFHEMConnection(new FHEMClientModeCon("/usr/bin/fhem.pl", 7072));
        Optional<FHEMModel> model_opt = parser.getFHEMModel();
        String sensorName = "HM_52CC96_Pwr";
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();
        assert model.sensorExists(sensorName);
        parser.setSensorPosition(DRÖLF, 2 * DRÖLF, sensorName);
        parser.execCommand("./pull.sh");
        parser.setFHEMConnection(new FHEMClientModeCon("/usr/bin/fhem.pl", 7072));
        model_opt = parser.getFHEMModel();
        assert model_opt.isPresent();
        Optional<FHEMSensor> sensor = model_opt.get().getSensorByName(sensorName);
        assert sensor.isPresent();
        System.out.println(sensor.get().getCoords().getX());
        assert sensor.get().getCoords().getX() == DRÖLF;
    }

    @Test
    public void testGetTimeserieDiscrete() {
        String sensorName = "HM_4F5DAA_Rain";
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        Optional<FHEMSensor> sensor = model.get().getSensorByName(sensorName);
        assert sensor.isPresent();
        Optional<FHEMFileLog> log = sensor.get().getLogs().stream().findFirst();
        assert log.isPresent();
        Optional<Timeserie> serie_opt = log.get().getTimeserie();
        assert serie_opt.isPresent();
        String json = new Gson().toJson(serie_opt.get(), Timeserie.class);
        Timeserie timeserie = new Gson().fromJson(json, Timeserie.class);
        assert timeserie.equals(serie_opt.get());
    }

    @Test
    public void testGetTimeserieReal() {
        String sensorName = "HM_521A72";
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        Optional<FHEMSensor> sensor = model.get().getSensorByName(sensorName);
        assert sensor.isPresent();
        Optional<FHEMFileLog> log = sensor.get().getLogs().stream().findFirst();
        assert log.isPresent();
        Optional<Timeserie> serie_opt = log.get().getTimeserie();
        assert serie_opt.isPresent();
        String json = new Gson().toJson(serie_opt.get(), Timeserie.class);
        Timeserie timeserie = new Gson().fromJson(json, Timeserie.class);
        assert timeserie.equals(serie_opt.get());
    }

    /**
     * Helper function which tests if a json string can be deserialized without throwing an exception
     * (which would mean incorrect json format).
     *
     * @param json A string which should be tested
     * @return whether the input was valid json
     */
    private static boolean isValidJSON(String json) {
        Gson gson = new Gson();
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }
}
