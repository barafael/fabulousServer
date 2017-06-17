package fhemModel.timeserie;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rafael on 16.06.17.
 */
public class DiscreteTimeserie extends Timeserie {
    private final HashMap<Integer, String> legend;
    private final List<Integer> ys;

    public DiscreteTimeserie(List<String> samples) {
        this.legend = new HashMap<>();
        /* avoid realloc */
        this.xs = new ArrayList<>(samples.size() + 5);
        this.ys = new ArrayList<>(samples.size() + 5);
        int currentKey = 0;
        for (String entry : samples) {
            String[] items = entry.split(" ");
            String value = items[3];
            if(!legend.containsValue(value)) {
                legend.put(currentKey++, value);
            }
            LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
            long epoch = dateTime.atZone(zoneId).toEpochSecond();
            xs.add(epoch);
            ys.add(currentKey);
        }
    }
}
