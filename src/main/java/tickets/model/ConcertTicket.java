package tickets.model;

import java.math.BigDecimal;
import java.util.Objects;

public record ConcertTicket(String eventId, int seat, BigDecimal price) implements Ticket {
    public ConcertTicket {
        Objects.requireNonNull(eventId); Objects.requireNonNull(price);
        if (eventId.isBlank() || seat < 1 || price.signum() < 0) throw new IllegalArgumentException("Invalid ticket");
    }
    public String category() { return "concert"; }
}
