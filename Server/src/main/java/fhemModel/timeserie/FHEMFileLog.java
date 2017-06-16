package fhemModel.timeserie;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fhemModel.sensors.Sensor;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 *
 * @author Rafael
 */

public class FHEMFileLog {
    private final Optional<Logtype> type;
    private Sensor sensor;
    private String sensorName;
    private String unit;
    private boolean isShowInApp;
    private String path;
    private static final DateTimeFormatter FHEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    public FHEMFileLog(String path, boolean isShowInApp) throws IOException {
        this.path = path;
        this.isShowInApp = isShowInApp;

        this.unit = getUnit().orElse("No unit given");
        this.sensorName = getSensorName().orElse("No sensor name given");
        System.out.println("constructing filelog: " + path);
        this.type = guessLogtype(path);
    }

    public Optional<? extends Timeserie> getTimeserie() {
        List<String> filelog;
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            filelog = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                filelog.add(line);
            }
            bufferedReader.close();

            Optional<Logtype> type_opt = guessLogtype(path);
            if (type_opt.isPresent()) {
                switch (type_opt.get()) {
                    case REALVAL:
                        return Optional.of(new RealValueTimeserie(filelog));
                    case PERCENTVAL:
                        return Optional.of(new PercentTimeserie(filelog));
                    case DISCRETEVAL:
                        return Optional.of(new DiscreteTimeserie(filelog));
                    default:
                        System.err.println("Unimplemented filelog type!");
                        return Optional.empty();
                }
            } else {
                System.err.println("Couldn't guess type of log! " + path);
                return Optional.empty();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<Logtype> guessLogtype(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            String line = bufferedReader.readLine();
            if (line.contains("%")) {
                return Optional.of(Logtype.PERCENTVAL);
            } else {
                String value = getValue(line);
                if (value.matches("[+-]?([0-9]+[.])?[0-9]+")) {
                    return Optional.of(Logtype.REALVAL);
                } else {
                    return Optional.of(Logtype.DISCRETEVAL);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Optional.empty();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private String getValue(String line) {
        String[] splitted = line.split(" ");
        if (splitted.length > 3) {
            return splitted[3];
        } else {
            return splitted[splitted.length - 1];
        }
    }

    public void setSensor(@NotNull Sensor s) {
        sensor = s;
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
            String name = line.split(" ")[1];
            return Optional.of(name);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Optional.empty();
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
            String unit = line.split(" ")[2];
            if (unit.endsWith(":")) {
                unit = unit.substring(0, unit.length() - 1);
            }
            return Optional.of(unit);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Optional.empty();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
