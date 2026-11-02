package tickets.factory;

import java.math.BigDecimal;
import tickets.model.ConcertTicket;
import tickets.model.Ticket;

public final class ConcertTicketFactory implements TicketFactory {
    public Ticket create(String eventId, int seat, BigDecimal price) {
        return new ConcertTicket(eventId, seat, price);
    }
}
