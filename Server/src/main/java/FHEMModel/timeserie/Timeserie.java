package FHEMModel.timeserie;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import FHEMModel.sensors.Sensor;
import com.sun.istack.internal.NotNull;
import parser.fhemJson.FHEMDevice;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 * @author Rafael
 */

public class Timeserie {
    private List<Sample> samples = new ArrayList<>();
    private Sensor sensor;
    private String sensorName;
    private String unit;
    private boolean isShowInApp;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    public Timeserie(List<String> filelog, boolean isShowInApp) {
        this.isShowInApp = isShowInApp;
        if (filelog.size() > 0) {
            sensorName = filelog.get(0).split(" ")[1];
            unit = filelog.get(0).split(" ")[2];
            if(unit.endsWith(":")) {
                unit = unit.substring(0, unit.length() - 1);
            }
            // System.out.println("Timeseries sensor name: " + sensorName + " unit:" + unit);
        }
        filelog.forEach(l -> samples.add(this.parseSample(l)));
    }

    Sample parseSample(String line) {
        String[] items = line.split(" ");
        double value = Double.parseDouble(items[3]);
        LocalDateTime dateTime = LocalDateTime.parse(items[0], formatter);
        return new Sample(dateTime, value);
    }

    public void setSensor(@NotNull Sensor s) {
        sensor = s;
    }

}
