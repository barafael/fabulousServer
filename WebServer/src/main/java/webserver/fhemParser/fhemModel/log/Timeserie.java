package webserver.fhemParser.fhemModel.log;

import com.google.gson.GsonBuilder;
import webserver.fhemParser.fhemModel.serializers.DoubleSerializer;

import java.time.Instant;
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
    private static final transient DateTimeFormatter FHEM_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    /**
     * A ZoneId, needed for timezone-independence.
     */
    private static final transient ZoneId ZONE_ID = ZoneId.systemDefault();
    /**
     * A pattern used to check if a string is parseable to a number (decimal or integer).
     */
    private static final transient Pattern NUMBER_PATTERN = Pattern.compile("[+-]?([0-9]+[.])?[0-9]+");
    /**
     * This size is used to limit the length of the timeserie.
     * Averaging is done in
     * {@link Timeserie#Timeserie(java.util.List, webserver.fhemParser.fhemModel.log.LogType) the constructor} and
     * {@link Timeserie#Timeserie(java.util.List, webserver.fhemParser.fhemModel.log.LogType, long, long)
     * the range constructor}
     */
    private static final int MAX_SIZE = 600;
    /**
     * A list of timestamps in unix long format.
     * Because this array is actually read by Gson, the mismatchedQueryAndUpdate warning is a false positive.
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Long> xs;
    /**
     * A list of readings parsed from a file.
     */
    private final List<Double> ys;

    /**
     * This legend associates certain y-values with names.
     * This can be used for 'open', 'closed' samples, or for markings on the y-axis,
     * or size limits.
     */
    private Map<Double, String> legend = new HashMap<>();

    /**
     * The oldest encountered unix timestamp.
     */
    private long oldestStamp;
    /**
     * The newest encountered unix timestamp.
     */
    private long newestStamp;

    /**
     * Constructor for a timeserie, which parses samples given in a list of strings.
     *
     * @param samples a list of strings directly from a FileLog
     * @param logType the desired filelog type
     */
    Timeserie(List<String> samples, LogType logType) {
        this(samples, logType, 0, Instant.now().getEpochSecond());
    }

    /**
     * Constructor for a timeserie, which parses samples given in a list of strings.
     *
     * @param samples a list of strings directly from a FileLog
     * @param logType the desired filelog type
     * @param start   unix timestamp to start with
     * @param end     unix timestamp to end with
     */
    Timeserie(List<String> samples, LogType logType, long start, long end) {
        this.legend = new HashMap<>();
        switch (logType) {
            case UNKNOWN:
            case DISCRETE:
                /* avoid realloc */
                xs = new ArrayList<>(samples.size() + 5);
                ys = new ArrayList<>(samples.size() + 5);
                double currentKey = 0;
                for (String entry : samples) {
                    String[] items = entry.split(" ");

                    LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
                    long epochSecond = dateTime.atZone(ZONE_ID).toEpochSecond();
                    if (epochSecond < start || epochSecond >= end) {
                        continue;
                    }
                    xs.add(epochSecond);

                    String value;
                    if (items.length <= 4) {
                        value = items[items.length - 1];
                    } else {
                        value = items[3];
                    }

                    if (!legend.containsValue(value)) {
                        legend.put(currentKey, value);
                        ys.add(currentKey);
                        currentKey++;
                    } else {
                        /* get() ok because legend.containsvalue(value) */
                        //noinspection ConstantConditions
                        ys.add(legend.entrySet().stream()
                                .filter(e -> e.getValue()
                                        .equals(value)).findFirst().get().getKey());
                    }
                }

                if (xs.size() > 1) {
                    oldestStamp = xs.get(0);
                    newestStamp = xs.get(xs.size() - 1);
                }

                legend.put(Collections.max(ys) + 1, "Upper");
                legend.put(Collections.min(ys) - 1, "Lower");

                break;

            case REAL:
            case PERCENT:
                /* avoid realloc */
                List<Long> local_xs = new ArrayList<>(samples.size() + 5);
                List<Double> local_ys = new ArrayList<>(samples.size() + 5);
                for (String entry : samples) {
                    String[] items = entry.split(" ");

                    LocalDateTime dateTime = LocalDateTime.parse(items[0], FHEM_DATE_FORMATTER);
                    long epochSecond = dateTime.atZone(ZONE_ID).toEpochSecond();
                    if (epochSecond < start || epochSecond >= end) {
                        continue;
                    }
                    local_xs.add(epochSecond);

                    double value = 0.0;
                    Matcher numberMatch = NUMBER_PATTERN.matcher(items[3]);
                    if (numberMatch.matches()) {
                        value = Double.parseDouble(items[3]);
                    }
                    local_ys.add(value);
                }

                if (local_xs.size() <= MAX_SIZE) {
                    xs = local_xs;
                    ys = local_ys;
                } else {
                    int k = local_xs.size() / MAX_SIZE;

                    long[] timestampQueue = new long[k];
                    double[] valueQueue = new double[k];

                    List<Long> newTimestamps = new ArrayList<>();
                    List<Double> newValues = new ArrayList<>();

                    for (int index = 0; index < local_xs.size(); index++) {
                        int minIndex = index % k;
                        timestampQueue[minIndex] = local_xs.get(index);
                        valueQueue[minIndex] = local_ys.get(index);
                        if (minIndex == 0) {
                            newTimestamps.add(Math.round(Arrays.stream(timestampQueue).average().orElse(0.0)));
                            newValues.add(Arrays.stream(valueQueue).average().orElse(0.0));
                        }
                    }
                    System.out.println("Reduced from " + local_xs.size() + " to "
                            + newTimestamps.size() + " elements!");
                    xs = newTimestamps;
                    ys = newValues;
                }

                if (xs.size() > 1) {
                    oldestStamp = xs.get(0);
                    newestStamp = xs.get(xs.size() - 1);
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
        return new GsonBuilder().registerTypeAdapter(Double.class, new DoubleSerializer()).create()
                .toJson(this, Timeserie.class);
    }
}
