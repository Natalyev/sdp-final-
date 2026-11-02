package tickets.model;

import java.math.BigDecimal;
import java.util.Objects;

public record FlightTicket(String eventId, int seat, BigDecimal price) implements Ticket {
    public FlightTicket {
        Objects.requireNonNull(eventId); Objects.requireNonNull(price);
        if (eventId.isBlank() || seat < 1 || price.signum() < 0) throw new IllegalArgumentException("Invalid ticket");
    }
    public String category() { return "flight"; }
}
