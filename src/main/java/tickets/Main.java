package tickets;

import java.math.BigDecimal;
import java.util.Set;
import tickets.factory.ConcertTicketFactory;
import tickets.factory.FlightTicketFactory;
import tickets.observer.AvailabilityBoard;
import tickets.observer.CustomerNotifier;
import tickets.observer.TicketEvent;
import tickets.observer.TicketEventPublisher;
import tickets.service.ReservationService;
import tickets.service.SeatInventory;
import tickets.strategy.BackRowStrategy;
import tickets.strategy.FrontRowStrategy;

public final class Main {
    public static void main(String[] args) {
        TicketEventPublisher events = new TicketEventPublisher();
        CustomerNotifier customer = new CustomerNotifier();
        AvailabilityBoard board = new AvailabilityBoard();
        events.subscribe(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, customer);
        events.subscribe(TicketEvent.Type.PRICE_CHANGED, customer);
        events.subscribe(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, board);

        SeatInventory concertSeats = new SeatInventory(Set.of(1, 2, 3), new FrontRowStrategy());
        ReservationService concert = new ReservationService(new ConcertTicketFactory(), concertSeats, events, new BigDecimal("50.00"));
        System.out.println("Factory + front-row strategy: " + concert.reserve("CONCERT-1"));
        concertSeats.setStrategy(new BackRowStrategy());
        System.out.println("Switched to back-row strategy: " + concert.reserve("CONCERT-1"));
        concert.changePrice("CONCERT-1", new BigDecimal("60.00"));

        ReservationService flight = new ReservationService(new FlightTicketFactory(),
                new SeatInventory(Set.of(10, 11), new BackRowStrategy()), events, new BigDecimal("200.00"));
        System.out.println("Flight factory: " + flight.reserve("FLIGHT-1"));
        System.out.println("Observer: customer received " + customer.received().size() + " events");
        System.out.println("Observer: last availability update has " + board.seatsRemaining() + " seat left");
    }
}
