package fhemModel.timeserie;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 * @author Rafael
 */

public class Timeserie {
    protected static final DateTimeFormatter FHEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    protected static final ZoneId zoneId = ZoneId.systemDefault();
    protected List<Long> xs;

    Pattern number = Pattern.compile("[+-]?([0-9]+[.])?[0-9]+");
}
