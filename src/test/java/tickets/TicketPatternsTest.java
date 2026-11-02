package tickets;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import tickets.factory.ConcertTicketFactory;
import tickets.factory.FlightTicketFactory;
import tickets.model.ConcertTicket;
import tickets.model.FlightTicket;
import tickets.observer.AvailabilityBoard;
import tickets.observer.CustomerNotifier;
import tickets.observer.Observer;
import tickets.observer.TicketEvent;
import tickets.observer.TicketEventPublisher;
import tickets.service.ReservationService;
import tickets.service.SeatInventory;
import tickets.strategy.BackRowStrategy;
import tickets.strategy.FrontRowStrategy;

class TicketPatternsTest {
    @Test void concertFactoryCreatesConcertTicket() {
        assertInstanceOf(ConcertTicket.class, new ConcertTicketFactory().create("SHOW", 1, new BigDecimal("50")));
    }
    @Test void flightFactoryCreatesFlightTicket() {
        assertInstanceOf(FlightTicket.class, new FlightTicketFactory().create("FLIGHT", 10, new BigDecimal("200")));
    }
    @Test void frontRowSelectsSmallestSeat() {
        assertEquals(2, new FrontRowStrategy().select(Set.of(2, 5, 9)));
    }
    @Test void backRowSelectsLargestSeat() {
        assertEquals(9, new BackRowStrategy().select(Set.of(2, 5, 9)));
    }
    @Test void strategyCanChangeDuringRuntime() {
        SeatInventory seats = new SeatInventory(Set.of(1, 2, 3), new FrontRowStrategy());
        assertEquals(1, seats.reserveSeat());
        seats.setStrategy(new BackRowStrategy());
        assertEquals(3, seats.reserveSeat());
    }
    @Test void noSeatCanBeReservedTwice() {
        SeatInventory seats = new SeatInventory(Set.of(1), new FrontRowStrategy());
        assertEquals(1, seats.reserveSeat());
        assertThrows(IllegalStateException.class, seats::reserveSeat);
    }
    @Test void observersReceiveOnlySubscribedEventTypes() {
        TicketEventPublisher events = new TicketEventPublisher();
        CustomerNotifier customer = new CustomerNotifier();
        events.subscribe(TicketEvent.Type.PRICE_CHANGED, customer);
        events.publish(new TicketEvent(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, "SHOW", 2, BigDecimal.TEN));
        events.publish(new TicketEvent(TicketEvent.Type.PRICE_CHANGED, "SHOW", 2, BigDecimal.TEN));
        assertEquals(1, customer.received().size());
    }
    @Test void observerCanUnsubscribeInsideHandler() {
        TicketEventPublisher events = new TicketEventPublisher();
        AtomicInteger count = new AtomicInteger();
        Observer[] observer = new Observer[1];
        observer[0] = event -> { count.incrementAndGet(); events.unsubscribe(TicketEvent.Type.PRICE_CHANGED, observer[0]); };
        events.subscribe(TicketEvent.Type.PRICE_CHANGED, observer[0]);
        TicketEvent change = new TicketEvent(TicketEvent.Type.PRICE_CHANGED, "SHOW", 2, BigDecimal.TEN);
        events.publish(change); events.publish(change);
        assertEquals(1, count.get());
    }
    @Test void reservationPublishesAvailability() {
        TicketEventPublisher events = new TicketEventPublisher();
        AvailabilityBoard board = new AvailabilityBoard();
        events.subscribe(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, board);
        ReservationService service = new ReservationService(new ConcertTicketFactory(),
                new SeatInventory(Set.of(1, 2), new FrontRowStrategy()), events, BigDecimal.TEN);
        assertEquals(1, service.reserve("SHOW").seat());
        assertEquals(1, board.seatsRemaining());
    }
    @Test void priceChangeNotifiesCustomer() {
        TicketEventPublisher events = new TicketEventPublisher();
        CustomerNotifier customer = new CustomerNotifier();
        events.subscribe(TicketEvent.Type.PRICE_CHANGED, customer);
        ReservationService service = new ReservationService(new FlightTicketFactory(),
                new SeatInventory(Set.of(10), new BackRowStrategy()), events, BigDecimal.TEN);
        service.changePrice("FLIGHT", new BigDecimal("15"));
        assertEquals(new BigDecimal("15"), customer.received().get(0).price());
        assertEquals(new BigDecimal("15"), service.reserve("FLIGHT").price());
    }
}
