package tickets.state;

import java.util.Objects;
import tickets.model.Ticket;

public record Reservation(String id, String customerId, Ticket ticket, ReservationStatus status) {
    public Reservation {
        if (id == null || id.isBlank() || customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("Reservation and customer IDs are required");
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(status);
    }
}
