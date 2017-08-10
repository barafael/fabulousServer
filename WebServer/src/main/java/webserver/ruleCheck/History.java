package webserver.ruleCheck;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Rafael
 */

public final class History {
    /**
     * How many changes of state will be recorded until the oldest is discarded.
     */
    private static final int CHANGE_THRESHOLD = 30;

    /**
     * A sequence of the last events.
     * <p>
     * Linked list because when the list is full old events have to be removed
     * This might cause reallocation with other list types.
     */
    private List<RuleEvent> events;

    public History() {
        this.events = new LinkedList<>();
    }

    /**
     * Add another ruleevent.
     *
     * If the list has {@link History#CHANGE_THRESHOLD CHANGE_THRESHOLD} elements,
     * the oldest element is discarded.
     * The list always remains sorted by {@link RuleEvent#getStartTime() startTime} after this operation.
     *
     * @param event the event to add
     */
    public void add(RuleEvent event) {
        if (events.size() >= CHANGE_THRESHOLD) {
            events.remove(events.size() - 1);
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

    /**
     * Remove all elements from the history.
     * Useful for unit tests.
     */
    public void clear() {
        events.clear();
    }
}
