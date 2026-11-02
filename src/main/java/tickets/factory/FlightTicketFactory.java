package tickets.factory;

import java.math.BigDecimal;
import tickets.model.FlightTicket;
import tickets.model.Ticket;

public final class FlightTicketFactory implements TicketFactory {
    public Ticket create(String eventId, int seat, BigDecimal price) {
        return new FlightTicket(eventId, seat, price);
    }
}
