package fhemModel.timeserie;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import fhemModel.sensors.Sensor;
import org.jetbrains.annotations.NotNull;

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
    private static final DateTimeFormatter FHEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    public Timeserie(List<String> filelog, boolean isShowInApp) {
        System.out.println(filelog.get(0));
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

    private Sample parseSample(String line) {
        double value = 0.0;
        String[] items = line.split(" ");
        String dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER).toString(); // better: unix timestamp
        if (items[3].matches("[+-]?([0-9]+[.])?[0-9]+")) {
            value = Double.parseDouble(items[3]);
        }
        switch (items[3]) {
            case "alive":
                value = 1.0;
                break;
            case "dead":
                value = 0.0;
                break;
        }
        return new Sample(dateTime, value);
    }

    public void setSensor(@NotNull Sensor s) {
        sensor = s;
    }

}
