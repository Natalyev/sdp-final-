package tickets.observer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AvailabilityBoard implements Observer {
    private final Map<String, Integer> byEvent = new ConcurrentHashMap<>();
    private volatile int seatsRemaining;
    public void onEvent(TicketEvent event) {
        if (event.type() != TicketEvent.Type.SEAT_AVAILABILITY_CHANGED) return;
        byEvent.put(event.eventId(), event.seatsRemaining());
        seatsRemaining = event.seatsRemaining();
    }
    public int seatsRemaining() { return seatsRemaining; }
    public Integer seatsRemaining(String eventId) { return byEvent.get(eventId); }
}
