package WebServer.FHEMParser.fhemModel.log;

import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 *
 * @author Rafael
 */

//TODO before final: privatize this, only public for testing
public class Timeserie {
    private transient static final DateTimeFormatter FHEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    /* Needed for timezone-independence */
    private transient static final ZoneId zoneId = ZoneId.systemDefault();
    private transient static final Pattern number = Pattern.compile("[+-]?([0-9]+[.])?[0-9]+");
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    /* Because this field is actually read by Gson */
    private final List<Long> xs;
    private final List<Double> ys;
    /**
     * This legend associates certain y-values with names.
     * This can be used for 'open', 'closed' samples, or for markings on the y-axis
     */
    private Map<Double, String> legend = new HashMap<>();

    /**
     * Constructor for a timeserie, which parses samples given in a list of strings.
     *
     * @param samples a list of strings directly from a FileLog
     * @param logtype the desired filelog type
     */
    Timeserie(List<String> samples, Logtype logtype) {
        this.legend = new HashMap<>();
        switch (logtype) {
            case UNKNOWN:
            case DISCRETE:
                /* avoid realloc */
                xs = new ArrayList<>(samples.size() + 5);
                ys = new ArrayList<>(samples.size() + 5);
                double currentKey = 0;
                for (String entry : samples) {
                    String[] items = entry.split(" ");

                    LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
                    long epoch = dateTime.atZone(zoneId).toEpochSecond();
                    xs.add(epoch);

                    String value = items[3];
                    if (!legend.containsValue(value)) {
                        legend.put(currentKey, value);
                        ys.add(currentKey);
                        currentKey++;
                    } else {
                        /* get() ok because legend.containsvalue(value) */
                        ys.add(legend.entrySet().stream()
                                .filter(e -> e.getValue()
                                        .equals(value)).findFirst().get().getKey());
                    }
                }
                legend.put(Collections.max(ys), "Max");
                legend.put(Collections.min(ys), "Min");
                break;
            case REAL:
            case PERCENT:
                /* avoid realloc */
                xs = new ArrayList<>(samples.size() + 5);
                ys = new ArrayList<>(samples.size() + 5);
                for (String entry : samples) {
                    String[] items = entry.split(" ");

                    LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
                    long epoch = dateTime.atZone(zoneId).toEpochSecond();
                    xs.add(epoch);

                    double value = 0.0;
                    Matcher numberMatch = number.matcher(items[3]);
                    if (numberMatch.matches()) {
                        value = Double.parseDouble(items[3]);
                    }
                    ys.add(value);
                }
                legend.put(Collections.max(ys), "Max");
                legend.put(Collections.min(ys), "Min");
                legend.put((Collections.min(ys) + Collections.max(ys)) / 2, "Middle");
                break;
            default:
                xs = new ArrayList<>();
                ys = new ArrayList<>();
        }
    }

    /**
     * Constructor for a timeserie, which parses samples given in a list of strings.
     *
     * @param samples a list of strings directly from a FileLog
     * @param logtype the desired filelog type
     * @param start   unix timestamp to start with
     * @param end     unix timestamp to end with
     */
    public Timeserie(List<String> samples, Logtype logtype, long start, long end) {
        this.legend = new HashMap<>();
        switch (logtype) {
            case UNKNOWN:
            case DISCRETE:
                /* avoid realloc */
                xs = new ArrayList<>(samples.size() + 5);
                ys = new ArrayList<>(samples.size() + 5);
                double currentKey = 0;
                for (String entry : samples) {
                    String[] items = entry.split(" ");

                    LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
                    long epoch = dateTime.atZone(zoneId).toEpochSecond();
                    if (epoch < start || epoch > end) {
                        continue;
                    }
                    xs.add(epoch);

                    String value = items[3];
                    if (!legend.containsValue(value)) {
                        legend.put(currentKey, value);
                        ys.add(currentKey);
                        currentKey++;
                    } else {
                        /* get() ok because legend.containsvalue(value) */
                        ys.add(legend.entrySet().stream()
                                .filter(e -> e.getValue()
                                        .equals(value)).findFirst().get().getKey());
                    }
                }
                break;
            case REAL:
            case PERCENT:
                /* avoid realloc */
                xs = new ArrayList<>(samples.size() + 5);
                ys = new ArrayList<>(samples.size() + 5);
                for (String entry : samples) {
                    String[] items = entry.split(" ");

                    LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
                    long epoch = dateTime.atZone(zoneId).toEpochSecond();
                    if (epoch < start || epoch > end) {
                        continue;
                    }
                    xs.add(epoch);

                    double value = 0.0;
                    Matcher numberMatch = number.matcher(items[3]);
                    if (numberMatch.matches()) {
                        value = Double.parseDouble(items[3]);
                    }
                    ys.add(value);
                }
                legend.put(Collections.max(ys), "Max");
                legend.put(Collections.min(ys), "Min");
                legend.put((Collections.min(ys) + Collections.max(ys)) / 2, "Middle");
                break;
            default:
                xs = new ArrayList<>();
                ys = new ArrayList<>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Timeserie timeserie = (Timeserie) o;

        return  xs.equals(timeserie.xs)
                && ys.equals(timeserie.ys)
                && legend.equals(timeserie.legend);
    }

    @Override
    public int hashCode() {
        int result = xs.hashCode();
        result = 31 * result + ys.hashCode();
        result = 31 * result + legend.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this, Timeserie.class);
    }
}
