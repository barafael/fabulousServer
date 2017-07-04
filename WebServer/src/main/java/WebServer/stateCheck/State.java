package WebServer.stateCheck;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rafael
 */

public class State {
    //TODO maybe add other variables here later
    private final Map<VAR, Long> times = new HashMap<>();
    /* GPS location for the FABLAB */
    private static final Location location = new Location("48.5657463", "13.450155799999948");
    private static final SunriseSunsetCalculator calculator =
            new SunriseSunsetCalculator(location, "Berlin/Europe");

    public State() {
        Calendar officialSunrise = calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance());
        Calendar officialSunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());
        times.put(VAR.SUNRISE, officialSunrise.getTimeInMillis()/1000L);
        times.put(VAR.SUNSET, officialSunset.getTimeInMillis()/1000L);
        times.put(VAR.STARTTIME, timeOfToday(6, 0));
        times.put(VAR.ENDTIME, timeOfToday(20, 0));
    }

    public Map<VAR, Long> getTimes() {
        return times;
    }

    @Override
    public String toString() {
        return "State{" +
                "times=" + times +
                '}';
    }

    private long timeOfToday(int hour, int minute) {
        Calendar time = new GregorianCalendar();
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTimeInMillis()/1000L;
    }
}
