package tickets.observer;

import java.math.BigDecimal;
import java.util.Objects;

public record TicketEvent(Type type, String eventId, int seatsRemaining, BigDecimal price) {
    public enum Type { SEAT_AVAILABILITY_CHANGED, PRICE_CHANGED }
    public TicketEvent {
        Objects.requireNonNull(type); Objects.requireNonNull(eventId); Objects.requireNonNull(price);
    }
}
