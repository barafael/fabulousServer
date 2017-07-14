package webserver.fhemParser.fhemModel.log;

import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 *
 * @author Rafael
 */
//TODO before final: privatize this, only public for testing
public final class Timeserie {
    /**
     * A formatter used to parse the FHEM date format.
     */
    private static final transient DateTimeFormatter FHEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    /**
     * A ZoneId, needed for timezone-independence.
     */
    private static final transient ZoneId ZONE_ID = ZoneId.systemDefault();
    /**
     * A pattern used to check if a string is parseable to a number, decimal or whole.
     */
    private static final transient Pattern NUMBER_PATTERN = Pattern.compile("[+-]?([0-9]+[.])?[0-9]+");
    private static final int MAX_SIZE = 2000;
    /**
     * A list of timestamps in unix long format.
     * Because this array is actually read by Gson, the mismatchedQueryAndUpdate warning is a false positive.
     */
    @SuppressWarnings ("MismatchedQueryAndUpdateOfCollection")
    private final List<Long> xs;
    /**
     * A list of readings parsed from a file.
     */
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
                    long epoch = dateTime.atZone(ZONE_ID).toEpochSecond();
                    xs.add(epoch);

                    String value = items[items.length - 1];
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
                legend.put(Collections.max(ys) + 1, "Upper");
                legend.put(Collections.min(ys) - 1, "Lower");
                break;
            case REAL:
            case PERCENT:
                /* avoid realloc */
                xs = new ArrayList<>(samples.size() + 5);
                ys = new ArrayList<>(samples.size() + 5);
                for (String entry : samples) {
                    String[] items = entry.split(" ");

                    LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
                    long epoch = dateTime.atZone(ZONE_ID).toEpochSecond();
                    xs.add(epoch);

                    double value = 0.0;
                    Matcher numberMatch = NUMBER_PATTERN.matcher(items[3]);
                    if (numberMatch.matches()) {
                        value = Double.parseDouble(items[3]);
                    }
                    ys.add(value);
                }

                legend.put(Collections.max(ys) + 1, "Upper");
                legend.put(Collections.min(ys) - 1, "Lower");

                if (xs.size() > MAX_SIZE) {
                    int k = xs.size() / MAX_SIZE;
                    long[] timestampQueue = new long[k];
                    double[] valueQueue = new double[k];

                    List<Long> newTimestamps = new ArrayList<>();
                    List<Double> newValues = new ArrayList<>();

                    for (int index = 0; index < xs.size(); index++) {
                        int minIndex = index % k;
                        timestampQueue[minIndex] = xs.get(index);
                        valueQueue[minIndex] = ys.get(index);
                        if (minIndex == 0) {
                            newTimestamps.add(Math.round(Arrays.stream(timestampQueue).average().orElse(0.0)));
                            newValues.add(Arrays.stream(valueQueue).average().orElse(0.0));
                        }
                    }
                    System.out.println("reduced from " + xs.size() + " to " + newTimestamps.size() + " elements!");
                    xs.clear();
                    xs.addAll(newTimestamps);
                    ys.clear();
                    ys.addAll(newValues);
                }
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
                    long epoch = dateTime.atZone(ZONE_ID).toEpochSecond();
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
                legend.put(Collections.max(ys) + 1, "Upper");
                legend.put(Collections.min(ys) - 1, "Lower");

                if (xs.size() > MAX_SIZE) {
                    int k = xs.size() / MAX_SIZE;
                    long[] timestampQueue = new long[k];
                    double[] valueQueue = new double[k];

                    List<Long> newTimestamps = new ArrayList<>();
                    List<Double> newValues = new ArrayList<>();

                    for (int index = 0; index < xs.size(); index++) {
                        int minIndex = index % k;
                        timestampQueue[minIndex] = xs.get(index);
                        valueQueue[minIndex] = ys.get(index);
                        if (minIndex == 0) {
                            newTimestamps.add(Math.round(Arrays.stream(timestampQueue).average().orElse(0.0)));
                            newValues.add(Arrays.stream(valueQueue).average().orElse(0.0));
                        }
                    }
                    System.out.println("reduced from " + xs.size() + " to " + newTimestamps.size() + " elements!");
                    xs.clear();
                    xs.addAll(newTimestamps);
                    ys.clear();
                    ys.addAll(newValues);
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
                    long epoch = dateTime.atZone(ZONE_ID).toEpochSecond();
                    if (epoch < start || epoch > end) {
                        continue;
                    }
                    xs.add(epoch);

                    double value = 0.0;
                    Matcher numberMatch = NUMBER_PATTERN.matcher(items[3]);
                    if (numberMatch.matches()) {
                        value = Double.parseDouble(items[3]);
                    }
                    ys.add(value);
                }
                legend.put(Collections.max(ys) + 1, "Upper");
                legend.put(Collections.min(ys) - 1, "Lower");
                break;
            default:
                xs = new ArrayList<>();
                ys = new ArrayList<>();
        }
    }

    @Override
    public int hashCode() {
        int result = xs.hashCode();
        result = 31 * result + ys.hashCode();
        result = 31 * result + legend.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Timeserie timeserie = (Timeserie) o;

        return xs.equals(timeserie.xs)
                && ys.equals(timeserie.ys)
                && legend.equals(timeserie.legend);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this, Timeserie.class);
    }
}
