package fhemModel.timeserie;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ra on 16.06.17.
 */
public class DiscreteTimeserie extends Timeserie {
    private List<Sample<Integer>> samples;

    private HashMap<Integer, String> legend;

    public DiscreteTimeserie(List<String> samples) {
        this.legend = new HashMap<>();
        this.samples = parseSamples(samples);
    }

    private List<Sample<Integer>> parseSamples(List<String> filelog) {
        this.samples = new ArrayList<>(filelog.size());
        int currentKey = 0;
        for (String entry : filelog) {
            String[] items = entry.split(" ");
            String value = items[3];
            if(!legend.containsValue(value)) {
                legend.put(currentKey++, value);
            }
            LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
            long epoch = dateTime.atZone(zoneId).toEpochSecond();
            samples.add(new Sample<>(epoch, currentKey));
        }
        return samples;
    }
}
