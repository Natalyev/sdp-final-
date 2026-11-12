package tickets.service;

import java.math.BigDecimal;
import java.util.Objects;
import tickets.factory.TicketFactory;
import tickets.model.Ticket;
import tickets.observer.Subject;
import tickets.observer.TicketEvent;

public final class ReservationService {
    private final TicketFactory factory;
    private final SeatInventory inventory;
    private final Subject events;
    private BigDecimal price;

    public ReservationService(TicketFactory factory, SeatInventory inventory, Subject events, BigDecimal initialPrice) {
        this.factory = Objects.requireNonNull(factory);
        this.inventory = Objects.requireNonNull(inventory);
        this.events = Objects.requireNonNull(events);
        this.price = validPrice(initialPrice);
    }

    public synchronized Ticket reserve(String eventId) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("Event ID is required");
        int seat = inventory.reserveSeat();
        Ticket ticket;
        try {
            ticket = factory.create(eventId, seat, price);
        } catch (RuntimeException failure) {
            inventory.releaseSeat(seat);
            throw failure;
        }
        events.publish(new TicketEvent(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, eventId, inventory.remaining(), price));
        return ticket;
    }

    public synchronized void changePrice(String eventId, BigDecimal newPrice) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("Event ID is required");
        price = validPrice(newPrice);
        events.publish(new TicketEvent(TicketEvent.Type.PRICE_CHANGED, eventId, inventory.remaining(), price));
    }

    public synchronized BigDecimal currentPrice() { return price; }

    public synchronized void release(Ticket ticket) {
        Objects.requireNonNull(ticket);
        inventory.releaseSeat(ticket.seat());
        events.publish(new TicketEvent(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED,
                ticket.eventId(), inventory.remaining(), price));
    }

    private static BigDecimal validPrice(BigDecimal value) {
        Objects.requireNonNull(value);
        if (value.signum() < 0) throw new IllegalArgumentException("Price cannot be negative");
        return value;
    }
}
