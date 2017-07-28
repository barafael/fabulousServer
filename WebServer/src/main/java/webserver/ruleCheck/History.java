package webserver.ruleCheck;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Rafael
 */

public class History {
    /**
     * How many changes of state will be recorded until the oldest is discarded.
     */
    private static final int CHANGE_THRESHHOLD = 30;

    /* Linked list because when the list is full old events have to be removed
    This might cause reallocation with other list types.
     */
    private List<RuleEvent> events;

    public History() {
        this.events = new LinkedList<>();
    }

    public void add(RuleEvent event) {
        if (events.size() >= CHANGE_THRESHHOLD) {
            events.remove(0);
        }
        events.add(event);
        events.sort(Comparator.comparingLong(RuleEvent::getStartTime).reversed());
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(events.size()).append(" events.\n");
        for (RuleEvent event : events) {
            stringBuilder.append('\t').append(event.toString()).append('\n');
        }
        return stringBuilder.toString();
    }

    public void clear() {
        events.clear();
    }
}
