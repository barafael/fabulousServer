package webserver.fhemParser.fhemModel.log;

import com.google.gson.GsonBuilder;
import webserver.fhemParser.fhemModel.serializers.DoubleSerializer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static webserver.fhemParser.fhemModel.log.LogType.DISCRETE;
import static webserver.fhemParser.fhemModel.log.LogType.PERCENT;
import static webserver.fhemParser.fhemModel.log.LogType.REAL;
import static webserver.fhemParser.fhemModel.log.LogType.UNKNOWN;

/**
 * This class is a proxy for an actual time serie, which can be parsed from disk on demand with
 * {@link FHEMFileLog#getTimeserie() getTimeserie()}.
 *
 * @author Rafael
 */
public final class FHEMFileLog {
    /* Json attributes, which are needed for deserialization.
       Static analysis reports false positives. */
    /**
     * The 'guessed' type of this log (discrete, percent, real, or unknown).
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final LogType type;

    /**
     * The name of this log, as set in FHEM.
     */
    private final String name;

    /**
     * The name of the corresponding sensor.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String sensorName;

    /**
     * The 'guessed' unit of this log.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String unit;

    /**
     * The path to the actual logfile on disk.
     */
    private final transient String path;

    /**
     * This string contains the permissions as read from FHEM.
     */
    private final transient List<String> permissions;

    /**
     * True if this filelog belongs to a device which can be toggled on/off.
     */
    private final transient boolean switchable;

    /**
     * Constructs a filelog from data obtained in fhemjson.
     *
     * @param path           path to an timeseries logfile
     * @param name           name of this filelog
     * @param switchable     whether this filelog has switchable permissions
     * @param permissions    the permission ID which this filelog requires to access it
     */
    public FHEMFileLog(String path, String name, boolean switchable, List<String> permissions) {
        this.path = path;
        this.name = name;
        this.switchable = switchable;
        this.unit = getUnitInFileLog(path).orElse("No unit given");
        this.sensorName = getSensorInFileLog(path).orElse("No sensor name given");
        this.type = guessLogtype(path);
        this.permissions = permissions;
    }

    public String getName() {
        return name;
    }

    /**
     * The filelogs in FHEM which are blessed should all be crafted in a way that the unit name appears
     * in the third column (the first being the date). This should be the FHEM default though.
     *
     * @param path the path to the FileLog. This has to be read as a file (instead of parsing it immediately)
     *             because parsing happens later on demand but the information is necessary now.
     * @return the name of the unit of this log
     */
    private static Optional<String> getUnitInFileLog(String path) {
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            line = bufferedReader.readLine();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
        if (line == null) {
            System.err.println("Could not read line in " + path + ". Presumably there are no entries in the log.");
            System.err.println("This can happen in the beginning of the month.");
            return Optional.empty();
        }
        String unit = line.split(" ")[2];
        if (unit.endsWith(":")) {
            unit = unit.substring(0, unit.length() - 1);
        }

        unit = unit.substring(0, 1).toUpperCase() + unit.substring(1);

        if (unit.equals("Co2")) {
            unit = "CO2";
        }
        /* Duck-type extra handling for switches */
        if (unit.equals("On") || unit.equals("Off")) {
            unit = "Switch";
        }

        return Optional.of(unit);
    }

    /**
     * The filelogs in FHEM which are blessed should all be crafted in a way that the sensor name appears
     * in the second column (the first being the date). This should be the FHEM default though.
     *
     * @param path the path to the FileLog. This has to be read as a file (instead of parsing it immediately)
     *             because parsing happens later on demand but the information is necessary now.
     * @return the name of the sensor this log belongs to
     */
    private static Optional<String> getSensorInFileLog(String path) {
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            line = bufferedReader.readLine();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        if (line == null) {
            System.err.println("Could not read line in " + path + ". Presumably there are no entries in the log.");
            System.err.println("This can happen in the beginning of the month.");
            return Optional.empty();
        }
        String name = line.split(" ")[1];
        return Optional.of(name);
    }

    /**
     * This method guesses the type of a log based on it's first line.
     * Types are defined in the logtypes enum.
     *
     * @param path path to logfile
     * @return an estimated logtype
     */
    private LogType guessLogtype(String path) {
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            line = bufferedReader.readLine();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return LogType.UNKNOWN;
        }
        if (line == null) {
            System.err.println("Could not read line in " + path + ". Presumably there are no entries in the log.");
            System.err.println("This can happen in the beginning of the month.");
            return UNKNOWN;
        }
        if (line.contains("%")) {
            return PERCENT;
        } else {
            String value = getLineValue(line);
            if (value.matches("[+-]?([0-9]+[.])?[0-9]+")) {
                return REAL;
            } else {
                return DISCRETE;
            }
        }
    }

    /**
     * This log is 'switchable' if the permission is set to A_.*
     * TODO update this
     * @return whether this log belongs to a switchable sensor
     */
    public boolean isSwitchable() {
        return switchable;
    }

    /**
     * A log belongs to a permitted switch if the necessary permissions exist and
     * start with A_.*
     * TODO update this
     * @param permissions
     * @return whether this is can be switched and is permitted
     */
    public boolean isPermittedSwitch(List<String> permissions) {
        for (String permission : this.permissions) {
            if (permissions.contains(permission) && isSwitchable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a line to obtain the value of the measurement.
     *
     * @param line line from a FileLog
     * @return the value, as a String
     */
    private String getLineValue(String line) {
        String[] splitted = line.split(" ");
        if (splitted.length > 3) {
            return splitted[3];
        } else {
            return splitted[splitted.length - 1];
        }
    }

    /**
     * Parses a timeserie from disk on demand.
     *
     * @return a parsed sample representation, if file is present
     */
    public Optional<Timeserie> getTimeserie() {
        List<String> lines;
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            lines = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
        LogType logType = guessLogtype(path);
        switch (logType) {
            case REAL:
            case PERCENT:
            case DISCRETE:
                return Optional.of(new Timeserie(lines, logType));
            case UNKNOWN:
                System.err.println("Couldn't guess type of log! " + path);
                return Optional.of(new Timeserie(lines, logType));
            default:
                System.err.println("Unimplemented logType! " + logType.name());
                return Optional.empty();
        }
    }

    /**
     * This method checks if given permissions are sufficient to access this filelog.
     *
     * @param allPermissions List of permissions which are available to a user
     * @return whether the user with given permissions is allowed to access this log
     */
    public boolean isPermitted(List<String> allPermissions) {
        for (String perm : permissions) {
            if (allPermissions.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a subsection of the entire timeserie based on start and end params.
     *
     * @param startTime the unix timestamp to start with
     * @param endTime   the unix timestamp to end with
     * @return a subsection of a timeserie
     */
    public Optional<String> subSection(long startTime, long endTime) {
        Optional<Timeserie> timeserie_opt = getTimeserie(startTime, endTime);
        return timeserie_opt.map(timeserie -> new GsonBuilder()
                .registerTypeAdapter(Double.class, new DoubleSerializer())
                .create()
                .toJson(timeserie, Timeserie.class));
    }

    /**
     * Parses a timeserie from disk on demand, between two unix timestamps.
     *
     * @param start the start date
     * @param end   the end date
     * @return a parsed sample representation, if file is present
     */
    private Optional<Timeserie> getTimeserie(long start, long end) {
        List<String> filelog;
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            filelog = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                filelog.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
        LogType logType = guessLogtype(path);
        switch (logType) {
            case REAL:
            case PERCENT:
            case DISCRETE:
                return Optional.of(new Timeserie(filelog, logType, start, end));
            case UNKNOWN:
                System.err.println("Couldn't guess type of log! " + path);
                return Optional.of(new Timeserie(filelog, logType, start, end));
            default:
                System.err.println("Unimplemented logType! " + logType.name());
                return Optional.empty();
        }

    }
}
