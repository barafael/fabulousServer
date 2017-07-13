package webserver.fhemParser.fhemModel.log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static webserver.fhemParser.fhemModel.log.Logtype.*;

/**
 * This class is a proxy for an actual timeserie, which can be parsed from disk on demand with {@link FHEMFileLog#getTimeserie() getTimeserie()}.
 *
 * @author Rafael
 */
public final class FHEMFileLog {
    /* Json attributes, which are needed for deserialization.
       Static analysis reports false positives. */
    @SuppressWarnings ("FieldCanBeLocal")
    private final Logtype type;
    private final String name;
    @SuppressWarnings ("FieldCanBeLocal")
    private final String sensorName;
    @SuppressWarnings ("FieldCanBeLocal")
    private final String unit;
    private final transient String path;

    /**
     * This string contains the permissions as read from FHEM.
     */
    private final transient List<String> permissions;

    /**
     * True if this filelog belongs to a device which can be toggled on/off.
     */
    private final boolean switchable;

    /**
     * Constructs a filelog from data obtained in fhemjson.
     *
     * @param path        path to an timeseries logfile
     * @param name        name of this filelog
     * @param switchable  whether this filelog has switchable permissions
     * @param isShowInApp whether this filelog should be exposed in the app (this is independent from permissions)
     * @param permissions the permission ID which this filelog requires to access it
     */
    public FHEMFileLog(String path, String name, boolean switchable, boolean isShowInApp, List<String> permissions) {
        this.path = path;
        this.name = name;
        this.switchable = switchable;
        this.unit = getUnitInFileLog(path).orElse("No unit given");
        this.sensorName = getSensorInFileLog(path).orElse("No sensor name given");
        this.type = guessLogtype(path);
        this.permissions = permissions;
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

        /* Duck-type extra handling for switches */
        if (unit.equals("on") || unit.equals("off")) {
            unit = "switch";
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
    private Logtype guessLogtype(String path) {
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            line = bufferedReader.readLine();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return Logtype.UNKNOWN;
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

    public boolean isSwitchable() {
        return switchable;
    }

    /**
     * Parses a line to obtain the value of the measurement.
     *
     * @param line line from a FileLog
     *
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

    public String getName() {
        return name;
    }

    /**
     * Parses a timeserie from disk on demand.
     *
     * @return a parsed sample representation, if file is present
     */
    public Optional<Timeserie> getTimeserie() {
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
        Logtype logtype = guessLogtype(path);
        switch (logtype) {
            case REAL:
            case PERCENT:
            case DISCRETE:
                return Optional.of(new Timeserie(filelog, logtype));
            case UNKNOWN:
                System.err.println("Couldn't guess type of log! " + path);
                return Optional.of(new Timeserie(filelog, logtype));
            default:
                System.err.println("Unimplemented logtype! " + logtype.name());
                return Optional.empty();
        }
    }

    /**
     * This method checks if given permissions are sufficient to access this filelog.
     *
     * @param allPermissions List of permissions which are available to a user
     *
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
        return timeserie_opt.map(timeserie -> new Gson().toJson(timeserie, Timeserie.class));
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
        Logtype logtype = guessLogtype(path);
        switch (logtype) {
            case REAL:
            case PERCENT:
            case DISCRETE:
                return Optional.of(new Timeserie(filelog, logtype, start, end));
            case UNKNOWN:
                System.err.println("Couldn't guess type of log! " + path);
                return Optional.of(new Timeserie(filelog, logtype, start, end));
            default:
                System.err.println("Unimplemented logtype! " + logtype.name());
                return Optional.empty();
        }

    }
}
