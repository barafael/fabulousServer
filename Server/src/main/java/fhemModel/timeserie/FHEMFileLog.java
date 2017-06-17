package fhemModel.timeserie;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fhemModel.sensors.FHEMSensor;
import org.jetbrains.annotations.NotNull;

import static fhemModel.timeserie.Logtype.*;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 *
 * @author Rafael
 */

public class FHEMFileLog {
    private final Logtype type;
    private final String name;
    private String sensorName;
    private String unit;
    private boolean isShowInApp;
    private String path;

    public FHEMFileLog(String path, String name, boolean isShowInApp) {
        this.path = path;
        this.isShowInApp = isShowInApp;
        this.name = name;
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

            Logtype type_opt = guessLogtype(path);
            switch (type_opt) {
                case REALVAL:
                    return Optional.of(new RealValueTimeserie(filelog));
                case PERCENTVAL:
                    return Optional.of(new PercentTimeserie(filelog));
                case DISCRETEVAL:
                    return Optional.of(new DiscreteTimeserie(filelog));
                case UNKNOWN:
                default:
                    System.err.println("Couldn't guess type of log! " + path);
                    return Optional.empty();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Logtype guessLogtype(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            String line = bufferedReader.readLine();
            bufferedReader.close();
            if (line == null) {
                System.err.println("Could not read line. Presumably there is none.");
                return UNKNOWN;
            }
            if (line.contains("%")) {
                return PERCENTVAL;
            } else {
                String value = getValue(line);
                if (value.matches("[+-]?([0-9]+[.])?[0-9]+")) {
                    return REALVAL;
                } else {
                    return DISCRETEVAL;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Logtype.UNKNOWN;
        } catch (IOException e) {
            e.printStackTrace();
            return Logtype.UNKNOWN;
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
            if (line == null) {
                System.err.println("Could not read line. Presumably there is none.");
                return Optional.empty();
            }
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

    public String getName() {
        return name;
    }
}
