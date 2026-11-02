package tickets.model;

import java.math.BigDecimal;

public sealed interface Ticket permits ConcertTicket, FlightTicket {
    String eventId();
    int seat();
    BigDecimal price();
    String category();
}
