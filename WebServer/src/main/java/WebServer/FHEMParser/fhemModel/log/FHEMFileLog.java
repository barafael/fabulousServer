package WebServer.FHEMParser.fhemModel.log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static WebServer.FHEMParser.fhemModel.log.Logtype.*;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 *
 * @author Rafael
 */

public class FHEMFileLog {
    transient private final Logtype type;
    private final String name;
    // TODO this is only here to force parsing for all logs. Remove to parse on demand with getTimeserie()
    // private Optional<? extends Timeserie> log = Optional.empty();
    private final String sensorName;
    private final String unit;
    transient private final boolean isShowInApp;
    transient private final String path;

    public FHEMFileLog(String path, String name, boolean isShowInApp) {
        this.path = path;
        this.isShowInApp = isShowInApp;
        this.name = name;
        this.unit = getUnit().orElse("No unit given");
        this.sensorName = getSensorName().orElse("No sensor name given");
        // System.out.println("constructing filelog: " + path);
        this.type = guessLogtype(path);

        // TODO this is only here to force parsing for all logs. Remove to parse on demand with getTimeserie()
        // this.timeserie = getTimeserie();
    }

    public String getName() {
        return name;
    }

    public Optional getTimeserie() {
        List<String> filelog;
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            filelog = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                filelog.add(line);
            }
            bufferedReader.close();

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
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Logtype guessLogtype(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            String line = bufferedReader.readLine();
            bufferedReader.close();
            if (line == null) {
                System.err.println("Could not read line. Presumably there is none.");
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
        } catch (IOException e) {
            e.printStackTrace();
            return Logtype.UNKNOWN;
        }
    }

    private String getLineValue(String line) {
        String[] splitted = line.split(" ");
        if (splitted.length > 3) {
            return splitted[3];
        } else {
            return splitted[splitted.length - 1];
        }
    }

    private Optional<String> getSensorName() {
        return FHEMFileLog.getSensorInFileLog(path);
    }

    private Optional<String> getUnit() {
        return FHEMFileLog.getUnitInFileLog(path);
    }

    private static Optional<String> getSensorInFileLog(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            String line = bufferedReader.readLine();
            bufferedReader.close();
            if (line == null) {
                System.err.println("Could not read line. Presumably there is none.");
                return Optional.empty();
            }
            String name = line.split(" ")[1];
            return Optional.of(name);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static Optional<String> getUnitInFileLog(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            String line = bufferedReader.readLine();
            bufferedReader.close();
            if (line == null) {
                System.err.println("Could not read line. Presumably there is none.");
                return Optional.empty();
            }
            String unit = line.split(" ")[2];
            if (unit.endsWith(":")) {
                unit = unit.substring(0, unit.length() - 1);
            }
            return Optional.of(unit);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
