package tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tickets.builder.EventConfiguration;
import tickets.facade.TicketReservationSystem;
import tickets.factory.ConcertTicketFactory;
import tickets.state.ReservationStatus;
import tickets.strategy.FrontRowStrategy;

class TicketFacadeTest {
    @Test void listsCurrentReservationsForOneCustomerInBookingOrder() {
        TicketReservationSystem system = new TicketReservationSystem();
        system.register(new EventConfiguration.Builder("SHOW")
                .factory(new ConcertTicketFactory())
                .strategy(new FrontRowStrategy())
                .seats(Set.of(1, 2, 3))
                .price(new BigDecimal("50.00"))
                .build());

        String first = system.book("SHOW", "C1").id();
        system.book("SHOW", "C2");
        String second = system.book("SHOW", "C1").id();
        system.confirm(first);

        var mine = system.reservationsForCustomer("C1");
        assertEquals(List.of(first, second), mine.stream().map(r -> r.id()).toList());
        assertEquals(List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING),
                mine.stream().map(r -> r.status()).toList());
        assertThrows(UnsupportedOperationException.class, () -> mine.clear());
        assertThrows(IllegalArgumentException.class, () -> system.reservationsForCustomer(" "));
    }
}
