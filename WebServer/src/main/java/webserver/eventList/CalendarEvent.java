package webserver.eventList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class CalendarEvent {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private long startStamp;
    private long endStamp;
    private String description;

    public CalendarEvent(String line) throws IllegalArgumentException {

        try {
            String[] dateAndDescription = line.split("\\s*;\\s*");
            String firstDate = dateAndDescription[0].split("\\s+")[0];
            String secondDate = dateAndDescription[0].split("\\s+")[1];
            this.description = dateAndDescription[1].trim();

            startStamp = parseStamp(firstDate);
            endStamp = parseStamp(secondDate);

            if (startStamp >= endStamp) {
                System.err.println("StartStamp was later than endStamp, swapping the dates...");
                long tmp = startStamp;
                startStamp = endStamp;
                endStamp = tmp;
            }
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            System.err.println("Cannot parse event: " + line);
            throw new IllegalArgumentException();
        }
    }

    private long parseStamp(String date) {
        LocalDateTime dateTime = LocalDateTime.parse(date, DATE_TIME_FORMATTER);
        return dateTime.atZone(ZONE_ID).toEpochSecond();
    }

    public long getStartStamp() {
        return startStamp;
    }

    public long getEndStamp() {
        return endStamp;
    }

    public String getDescription() {
        return description;
    }
}
