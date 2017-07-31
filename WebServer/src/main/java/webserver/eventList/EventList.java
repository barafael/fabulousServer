package webserver.eventList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EventList {
    private final List<CalendarEvent> events = new ArrayList<>();

    public EventList(String path) {
        try {
            File f = new File(path);
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    CalendarEvent event = new CalendarEvent(line);
                    events.add(event);
                } catch (IllegalArgumentException iae) {
                    System.err.println("Ignored event line: " + line);
                }
            }
            events.sort(Comparator.comparingLong(CalendarEvent::getStartStamp));
            long now = Instant.now().getEpochSecond();
            events.removeIf(event -> event.getEndStamp() < now);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<CalendarEvent> getEvents() {
        return events;
    }
}
