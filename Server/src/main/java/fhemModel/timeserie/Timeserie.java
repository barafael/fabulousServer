package fhemModel.timeserie;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import fhemModel.sensors.Sensor;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 * @author Rafael
 */

public class Timeserie {
    protected static final DateTimeFormatter FHEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    protected static final ZoneId zoneId = ZoneId.systemDefault();
}
