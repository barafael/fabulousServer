package WebServer.FHEMParser.fhemModel.timeserie;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 *
 * @author Rafael
 */

public class Timeserie {
    transient static final DateTimeFormatter FHEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    transient static final ZoneId zoneId = ZoneId.systemDefault();

    /* TODO: Maybe completely disregard floating point values? Int everything. */
    final BiMap<Double, String> legend;

    List<Long> xs;
    List<Double> ys;

    transient static final Pattern number = Pattern.compile("[+-]?([0-9]+[.])?[0-9]+");

    Timeserie(List<String> samples, Logtype logtype) {
        this.legend = HashBiMap.create();
        switch (logtype) {
            case UNKNOWN:
            case DISCRETE:
                /* avoid realloc */
                xs = new ArrayList<>(samples.size() + 5);
                ys = new ArrayList<>(samples.size() + 5);
                double currentKey = 0;
                for (String entry : samples) {
                    String[] items = entry.split(" ");
                    String value = items[3];
                    if (!legend.containsValue(value)) {
                        legend.put(currentKey, value);
                        currentKey++;
                    }
                    ys.add(legend.inverse().get(value));
                    LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
                    long epoch = dateTime.atZone(zoneId).toEpochSecond();
                    xs.add(epoch);
                }
                break;
            case REAL:
            case PERCENT:
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
                legend.inverse().put("Max", Collections.max(ys));
                legend.inverse().put("Min", Collections.min(ys));
                legend.inverse().put("Center", (Collections.min(ys) + Collections.max(ys)) / 2);
                break;
            default:
                xs = new ArrayList<>();
                ys = new ArrayList<>();
        }
    }
}