package fhemModel.timeserie;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ra on 16.06.17.
 */
public class PercentTimeserie extends Timeserie {
    private List<Sample<Double>> samples;

    public PercentTimeserie(List<String> samples) {
        this.samples = parseSamples(samples);
    }

    private List<Sample<Double>> parseSamples(List<String> filelog) {
        this.samples = new ArrayList<>(filelog.size());
        for (String entry : filelog) {
            String[] items = entry.split(" ");
            double value = 0.0;
            if (items[3].matches("[+-]?([0-9]+[.])?[0-9]+")) {
                value = Double.parseDouble(items[3]);
            }
            LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
            long epoch = dateTime.atZone(zoneId).toEpochSecond();
            samples.add(new Sample<>(epoch, value));
        }
        return samples;
    }
}
