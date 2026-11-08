package tickets;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tickets.builder.EventConfiguration;
import tickets.facade.TicketReservationSystem;
import tickets.factory.ConcertTicketFactory;
import tickets.factory.FlightTicketFactory;
import tickets.model.ConcertTicket;
import tickets.model.FlightTicket;
import tickets.observer.AvailabilityBoard;
import tickets.observer.CustomerNotifier;
import tickets.observer.TicketEvent;
import tickets.state.Reservation;
import tickets.state.ReservationStatus;
import tickets.strategy.BackRowStrategy;
import tickets.strategy.FrontRowStrategy;

class TicketWorkflowTest {
    private EventConfiguration concert(String id, Set<Integer> seats) {
        return new EventConfiguration.Builder(id).factory(new ConcertTicketFactory())
                .strategy(new FrontRowStrategy()).seats(seats).price(new BigDecimal("50.00")).build();
    }

    @Test void builderRejectsIncompleteOrInvalidEvent() {
        assertThrows(NullPointerException.class, () -> new EventConfiguration.Builder("X").build());
        assertThrows(IllegalArgumentException.class, () -> concert("X", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new EventConfiguration.Builder("X")
                .factory(new ConcertTicketFactory()).strategy(new FrontRowStrategy())
                .seats(Set.of(1)).price(new BigDecimal("-1")).build());
    }

    @Test void facadeIntegratesBuilderFactoryStrategyStateAndObserver() {
        TicketReservationSystem system = new TicketReservationSystem();
        AvailabilityBoard board = new AvailabilityBoard();
        CustomerNotifier customer = new CustomerNotifier();
        system.subscribe(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, board);
        system.subscribe(TicketEvent.Type.PRICE_CHANGED, customer);
        system.register(concert("SHOW", Set.of(1, 2, 3)));

        Reservation first = system.book("SHOW", "C1");
        assertInstanceOf(ConcertTicket.class, first.ticket());
        assertEquals(1, first.ticket().seat());
        assertEquals(ReservationStatus.PENDING, first.status());
        assertEquals(2, board.seatsRemaining());

        assertEquals(ReservationStatus.CONFIRMED, system.confirm(first.id()).status());
        assertEquals(ReservationStatus.PENDING, first.status());
        system.setStrategy("SHOW", new BackRowStrategy());
        Reservation second = system.book("SHOW", "C2");
        assertEquals(3, second.ticket().seat());

        assertEquals(ReservationStatus.CANCELLED, system.cancel(second.id()).status());
        assertEquals(2, system.remaining("SHOW"));
        assertEquals(2, board.seatsRemaining());
        assertThrows(IllegalStateException.class, () -> system.confirm(second.id()));
        assertThrows(IllegalStateException.class, () -> system.cancel(second.id()));

        system.changePrice("SHOW", new BigDecimal("60.00"));
        assertEquals(new BigDecimal("60.00"), customer.received().get(0).price());
    }

    @Test void cancelledSeatCanBeBookedAgainOnlyOnce() {
        TicketReservationSystem system = new TicketReservationSystem();
        system.register(concert("SHOW", Set.of(1)));
        Reservation first = system.book("SHOW", "C1");
        assertThrows(IllegalStateException.class, () -> system.book("SHOW", "C2"));
        system.cancel(first.id());
        assertEquals(1, system.book("SHOW", "C2").ticket().seat());
        assertEquals(0, system.remaining("SHOW"));
    }

    @Test void eventsHaveIndependentSeatInventoriesAndTicketFactories() {
        TicketReservationSystem system = new TicketReservationSystem();
        system.register(concert("SHOW", Set.of(1, 2)));
        system.register(new EventConfiguration.Builder("FLIGHT").factory(new FlightTicketFactory())
                .strategy(new BackRowStrategy()).seats(Set.of(10, 11))
                .price(new BigDecimal("200.00")).build());
        assertInstanceOf(ConcertTicket.class, system.book("SHOW", "C1").ticket());
        assertInstanceOf(FlightTicket.class, system.book("FLIGHT", "C2").ticket());
        assertEquals(11, system.reservation("R-2").ticket().seat());
        assertEquals(1, system.remaining("SHOW"));
        assertEquals(1, system.remaining("FLIGHT"));
    }

    @Test void duplicateAndUnknownIdsAreRejected() {
        TicketReservationSystem system = new TicketReservationSystem();
        system.register(concert("SHOW", Set.of(1)));
        assertThrows(IllegalArgumentException.class, () -> system.register(concert("SHOW", Set.of(2))));
        assertThrows(IllegalArgumentException.class, () -> system.book("UNKNOWN", "C1"));
        assertThrows(IllegalArgumentException.class, () -> system.confirm("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> system.book("SHOW", " "));
    }
}
