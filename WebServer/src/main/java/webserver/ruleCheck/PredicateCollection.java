package webserver.ruleCheck;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A collection of predicates which can be called from a general predicate rule.
 * Predicates must be public, return boolean, and take a List<String> (even if they ignore it).
 *
 * @author Rafael
 */

public class PredicateCollection {
    /* FabLab GPS location */
    private static final Location location = new Location("48.5657463", "13.450155799999948");
    private static final SunriseSunsetCalculator calculator =
            new SunriseSunsetCalculator(location, "Vienna/Europe");
    private final Calendar officialSunrise = calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance());
    private final Calendar officialSunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());

    /**
     * An example predicate for use with the rule checker. See testGeneralPredicate() for usage.
     * (Not linkable from here because it is a unit test).
     * It is not unused, but since it is called at runtime via reflection, it cannot be detected at compile time.
     * <p>
     * Predicates must be public, return boolean, and take a List<String> (even if they ignore it).
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
     * A predicate to check if hours are between 6am and 8pm.
     * @param _ignored ignored parameter list
     * @return whether the current time is a working hour
     */
    public boolean isWorkingHours(@SuppressWarnings("unused") List<String> _ignored) {
        long startTime = timeOfTodayInUNIXSeconds(6, 0);
        long endTime = timeOfTodayInUNIXSeconds(20, 0);
        long now = Instant.now().getEpochSecond();
        // TODO handle weekdays, weekends, holidays, and free days in own predicate or here
        return now > startTime && now < endTime;
    }

    /**
     * A predicate to check if the sun is up at this moment.
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
     * @param _ignored ignored parameter list
     * @return always true
     */
    @SuppressWarnings({"unused", "SameReturnValue"})
    public boolean noWeatherWarnings(@SuppressWarnings("unused") List<String> _ignored) {
        // Just an idea...
        // https://openweathermap.org/triggers
        return true;
    }

    /**
     * Calculates a time of the current day as absolute unix seconds.
     * If hours and minutes are not in range, their modulo is used.
     *
     * @param hour an int from 0(inclusive) to 24(exclusive)
     * @param minute an int from 0(inclusive) to 60(exclusive)
     * @return the absolute unix timestamp of the hour and minute given on this day
     */
    private long timeOfTodayInUNIXSeconds(int hour, int minute) {
        Calendar time = new GregorianCalendar();
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTimeInMillis() / 1000L;
    }
}
