package fhemModel.timeserie;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rafael on 16.06.17.
 */
public class PercentTimeserie extends Timeserie<Double> {
    public PercentTimeserie(List<String> samples) {
        /* avoid realloc */
        xs = new ArrayList<>(samples.size() + 5);
        ys = new ArrayList<>(samples.size() + 5);
        for (String entry : samples) {
            String[] items = entry.split(" ");
            double value = 0.0;
            Matcher numberMatch = number.matcher(items[3]);
            if (numberMatch.matches()) {
                value = Double.parseDouble(items[3]);
            }
            LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
            long epoch = dateTime.atZone(zoneId).toEpochSecond();
            xs.add(epoch);
            ys.add(value);
        }
    }
}
