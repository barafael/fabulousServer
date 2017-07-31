package webserver.eventList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CalendarEvent {
    private long startStamp;
    private long endStamp;
    private String description;

    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm");
    private final static ZoneId zoneId = ZoneId.systemDefault();

    public CalendarEvent(String line) throws IllegalArgumentException {

        try {
            String[] dateAndDescription = line.split(";");
            String firstDate = dateAndDescription[0].split(" ")[0];
            String secondDate = dateAndDescription[0].split(" ")[1];
            this.description = dateAndDescription[1];

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
        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
        return dateTime.atZone(zoneId).toEpochSecond();
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
