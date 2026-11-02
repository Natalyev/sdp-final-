package tickets.factory;

import java.math.BigDecimal;
import tickets.model.Ticket;

public interface TicketFactory {
    Ticket create(String eventId, int seat, BigDecimal price);
}
