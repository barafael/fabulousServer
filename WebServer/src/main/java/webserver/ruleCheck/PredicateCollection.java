package webserver.ruleCheck;

/* Sunrise and sunset calculator.
 * Apache 2.0
 */
//https://github.com/mikereedell/sunrisesunsetlib-java

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import de.jollyday.Holiday;
import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

/**
 * A collection of predicates which can be called from a general predicate rule.
 * Predicates must be public, return boolean, and take a List&lt;String&gt; (even if they ignore it).
 *
 * @author Rafael
 */

public final class PredicateCollection {
    /**
     * FabLab GPS location.
     */
    private static final Location LOCATION = new Location("48.5657463", "13.450155799999948");
    /**
     * Localised sun calculator.
     */
    private static final SunriseSunsetCalculator CALCULATOR =
            new SunriseSunsetCalculator(LOCATION, "Vienna/Europe");
    /**
     * Localised sunrise calculator.
     */
    private final Calendar officialSunrise = CALCULATOR.getOfficialSunriseCalendarForDate(Calendar.getInstance());
    /**
     * Localised sunset calculator.
     */
    private final Calendar officialSunset = CALCULATOR.getOfficialSunsetCalendarForDate(Calendar.getInstance());

    /**
     * An example predicate for use with the rule checker. See testGeneralPredicate() for usage.
     * (Not linkable from here because it is a unit test).
     * It is not unused, but since it is called at runtime via reflection, it cannot be detected at compile time.
     * <p>
     * Predicates must be public, return boolean, and take a List&lt;String&gt; (even if they ignore it).
     *
     * @param arguments the strings defined in the input file
     * @return true, always (to make it more useful, make it return something)
     */
    @SuppressWarnings({"unused", "SameReturnValue"})
    public boolean exampleAlwaysTruePredicate(List<String> arguments) {
        arguments.forEach(s -> System.out.print(s + " "));
        System.out.println();
        return true;
    }

    /**
     * A predicate which is true only every second minute.
     *
     * @param _ignored ignored parameter list
     * @return whether the current minute is divisible by 2, true if yes
     */
    public boolean isEvenMinute(@SuppressWarnings("unused") List<String> _ignored) {
        Calendar now = Calendar.getInstance();
        int minute = now.get(Calendar.MINUTE);
        return minute % 2 == 0;
    }

    /**
     * A predicate to check if hours are between 6am and 8pm.
     *
     * @param _ignored ignored parameter list
     * @return whether the current time is a working hour
     */
    public boolean isWorkingHours(@SuppressWarnings("unused") List<String> _ignored) {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            return false;
        }
        HolidayManager m = HolidayManager.getInstance(HolidayCalendar.GERMANY);

        // For munich and bavaria, jollyday does not do passau
        Set<Holiday> holidays = m.getHolidays(Calendar.getInstance().get(Calendar.YEAR), "by", "mu");

        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(DateTimeZone.forID("Europe/Berlin"));

        // City and Region codes:
        // http://jollyday.sourceforge.net/data/de.html
        boolean isHoliday = holidays.stream()
                .map(Holiday::getDate)
                .filter(localDate -> localDate.equals(today))
                .count() > 0;
        if (isHoliday) {
            return false;
        }
        long startTime = timeOfTodayInUNIXSeconds(6, 0);
        long endTime = timeOfTodayInUNIXSeconds(20, 0);
        long now = Instant.now().getEpochSecond();
        return now > startTime && now < endTime;
    }

    /**
     * A predicate to check if the sun is up at this moment.
     *
     * @param _ignored ignored parameter list
     * @return whether the sun is up at this moment
     */
    public boolean sunIsUp(@SuppressWarnings("unused") List<String> _ignored) {
        long sunrise = officialSunrise.getTimeInMillis() / 1000L;
        long sunset = officialSunset.getTimeInMillis() / 1000L;
        long now = Instant.now().getEpochSecond();
        return now > sunrise && now < sunset;
    }

    /**
     * Weather warning predicate, only to show the concept.
     *
     * @param _ignored ignored parameter list
     * @return always true
     */
    @SuppressWarnings({"unused", "SameReturnValue"})
    public boolean weatherWarnings(@SuppressWarnings("unused") List<String> _ignored) {
        // Just an idea...
        // https://openweathermap.org/triggers
        return true;
    }

    /**
     * Calculates a time of the current day as absolute unix seconds.
     * If hours and minutes are not in range, their modulo is used.
     *
     * @param hour   an int from 0(inclusive) to 24(exclusive)
     * @param minute an int from 0(inclusive) to 60(exclusive)
     * @return the absolute unix timestamp of the hour and minute given on this day
     */
    private long timeOfTodayInUNIXSeconds(int hour, int minute) {
        hour = hour % 24;
        minute = minute % 60;

        Calendar time = new GregorianCalendar();
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTimeInMillis() / 1000L;
    }
}
