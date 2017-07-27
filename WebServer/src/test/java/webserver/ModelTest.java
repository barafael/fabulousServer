package webserver;

import com.google.gson.Gson;
import org.junit.BeforeClass;
import org.junit.Test;
import webserver.fhemParser.FHEMParser;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.log.Timeserie;
import webserver.fhemParser.fhemModel.room.FHEMRoom;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * This class contains tests for the FHEM Model.
 * <p>
 * All tests which get a FHEM model depend on local copies of FHEM state files.
 * Alternatively, the pullData function (annotated with @BeforeClass) pulls the data if it is not present.
 *
 * @author Rafael
 */
public class ModelTest {
    /**
     * An integer between 0 and 100 used for testing hashes and position.
     */
    private static final int RANDOM = new Random().nextInt(101);

    /**
     * Conditionally pulls the mocking data to the local machine (if it is not yet present).
     * This can take a moment, depending on load and traffic.
     *
     * @throws IOException if there was an I/O error during command execution.
     */
    @BeforeClass
    public static void pullData() throws IOException {
        if (Files.exists(Paths.get("/tmp/fhemlog/"))) {
            return;
        }
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", "./pull.sh"});
        try (BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()))) {
            /* Wait until process is done */
            stdin.readLine();
        }
    }

    /**
     * Helper method which writes a FHEM model to a file for manual inspection.
     * Additionally, it is tested that the FHEM state files are present and other tests can proceed.
     */
    @Test
    public void modelPrint() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();
        Path path = Paths.get("fhemmodel.json");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(model.toString());
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
        }
    }

    /**
     * Test the model iterator.
     */
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

    /**
     * Test the sensor iterator.
     * Every sensor in the entire model is iterated.
     */
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

    /**
     * Test the filelog iterator.
     * Every filelog in the entire model is iterated.
     */
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

    /**
     * This test checks the time it takes to parse the raw FHEM model without handling any serialization or permissions.
     */
    @Test
    public void modelParseTimeWithoutPermissions() {
        Instant now = Instant.now();
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        if (model_opt.isPresent()) {
            System.out.println("Without permissions or serialization: "
                    + Duration.between(now, Instant.now()).toMillis());
        } else {
            assert false;
        }
    }

    /**
     * This test checks the time it takes to parse the raw FHEM model and handle serialization and permissions.
     */
    @Test
    public void modelParseTimeWithPermissions() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        if (model_opt.isPresent()) {
            Instant now = Instant.now();
            Optional<String> json = FHEMParser.getInstance().getFHEMModelJSON(Arrays.asList(
                    "permission1", "S_Fenster"), Collections.emptyList());
            json.ifPresent(fhemRooms -> System.out.println(
                    "With permissions: " + Duration.between(now, Instant.now()).toMillis()));
        } else {
            assert false;
        }
    }

    /**
     * This test checks the time it takes to parse the raw FHEM model and handle permissions.
     */
    @Test
    public void parseWithoutPermissionsJSON() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        assert isValidJSON(model.get().toJson());
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

    /**
     * Assert that the toString() of FHEMModel returns valid json.
     */
    @Test
    public void modelWithoutPermissionsToString() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        assert isValidJSON(model.get().toString());
    }

    /**
     * Test the model as JSON with permission handling.
     */
    @Test
    public void toJSONWithPermissions() {
        List<String> permissions = Arrays.asList("Permission1", "S_Fenster");
        Optional<String> json = FHEMParser.getInstance().getFHEMModelJSON(permissions, Collections.emptyList());
        assert json.isPresent();
        assert isValidJSON(json.get());
    }

    /**
     * Test the model as JSON with emtpy string as permission.
     */
    @Test
    public void toJSONWithEmptyStringPermissions() {
        List<String> permissions = Collections.singletonList("");
        Optional<String> json = FHEMParser.getInstance().getFHEMModelJSON(permissions, Collections.emptyList());
        assert json.isPresent();
        assert isValidJSON(json.get());
    }

    /**
     * Test that a switchable sensor is correctly detected.
     */
    @Test
    public void testSwitchableSensor() {
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();

        assert model.get().getSensorByName("HM_52CB8E_Sw").isPresent();
        assert model.get().getSensorByName("HM_52CB8E_Sw").get().isSwitchable();
    }

    /**
     * Serialize and deserialize again, comparing the results.
     */
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

    /**
     * Test model serialization with permissions which are not fulfilled.
     */
    @Test
    public void toJSONWithPermissionsNoMatch() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Arrays.asList("nomatch", "othernomatch");
        Optional<String> json = parser.getFHEMModelJSON(permissions, Collections.emptyList());
        assert json.isPresent();
        assert isValidJSON(json.get());
        /* There was no match! */
        assert json.get().equals("null");
    }

    //@Test
    public void getRoomNoHash() {
        FHEMParser parser = FHEMParser.getInstance();
        assert parser.getFHEMModel().isPresent();
        List<String> permissions = Arrays.asList("testing", "otherperm");
        Optional<String> room = parser.getRoomplan("room_testing", permissions);
        assert room.isPresent();
        assert room.get().equals("that's no moon.\n");
    }

    //@Test
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

    //@Test
    public void getRoomWithUnequalHash() {
        FHEMParser parser = FHEMParser.getInstance();
        List<String> permissions = Arrays.asList("testing", "otherperm");
        String content = "that's no moon.\n";
        parser.getFHEMModel();
        parser.setRoomplan("room_testing", content);
        Optional<String> roomplan = parser.getRoomplan("room_testing", RANDOM, permissions);
        assert roomplan.isPresent();
        assert roomplan.get().equals(content);
    }

    //@Test
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

    /**
     * Test getting a discrete timeserie.
     */
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

    /**
     * Test getting a real timeserie via the same interface that the server uses.
     */
    @Test
    public void testGetTimeserieRealFromParser() {
        String sensorName = "HM_521A72";
        List<String> permissions = Collections.singletonList("S_Licht");
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        Optional<FHEMSensor> sensor = model.get().getSensorByName(sensorName);
        assert sensor.isPresent();
        Optional<String> logname = sensor.get().getLogs().stream().findFirst().map(FHEMFileLog::getName);
        assert logname.isPresent();
        Optional<String> json_opt = FHEMParser.getInstance().getTimeserie(logname.get(), permissions);
        assert json_opt.isPresent();
    }

    /**
     * Test getting a real timeserie using the internal interface.
     */
    @Test
    public void testGetTimeserieRealInternal() {
        String sensorName = "HM_52CB59_Sw";
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
     * Test getting the timeserie of a window.
     */
    @Test
    public void testGetTimeserieWindow() {
        String sensorName = "HM_56A27C";
        List<String> permissions = Collections.singletonList("S_Fenster");
        Optional<FHEMModel> model = FHEMParser.getInstance().getFHEMModel();
        assert model.isPresent();
        Optional<FHEMSensor> sensor = model.get().getSensorByName(sensorName);
        assert sensor.isPresent();
        Optional<String> logname = sensor.get().getLogs().stream().findFirst().map(FHEMFileLog::getName);
        assert logname.isPresent();
        Optional<String> json_opt = FHEMParser.getInstance().getTimeserie(logname.get(), permissions);
        assert json_opt.isPresent();
    }
}
