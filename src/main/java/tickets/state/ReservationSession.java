package tickets.state;

import java.util.Objects;
import tickets.model.Ticket;

public final class ReservationSession {
    private final String id;
    private final String customerId;
    private final Ticket ticket;
    private ReservationState state = new PendingState();

    public ReservationSession(String id, String customerId, Ticket ticket) {
        if (id == null || id.isBlank() || customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("Reservation and customer IDs are required");
        this.id = id;
        this.customerId = customerId;
        this.ticket = Objects.requireNonNull(ticket);
    }

    public synchronized Reservation snapshot() { return new Reservation(id, customerId, ticket, state.status()); }
    public synchronized void confirm() { state = state.confirm(); }
    public synchronized void cancel() { state = state.cancel(); }
}
