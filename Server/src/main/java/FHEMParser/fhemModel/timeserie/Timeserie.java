package FHEMParser.fhemModel.timeserie;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 * @author Rafael
 */

public abstract class Timeserie<T extends Number> {
    transient protected static final DateTimeFormatter FHEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    transient protected static final ZoneId zoneId = ZoneId.systemDefault();
    protected final BiMap<Integer, String> legend;

    protected List<Long> xs;
    protected List<T> ys;

    transient protected static final Pattern number = Pattern.compile("[+-]?([0-9]+[.])?[0-9]+");

    public Timeserie() {
        this.legend = HashBiMap.create();
    }
}
