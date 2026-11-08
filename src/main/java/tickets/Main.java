package tickets;

import java.math.BigDecimal;
import java.util.Set;
import tickets.builder.EventConfiguration;
import tickets.cli.ConsoleApp;
import tickets.facade.TicketReservationSystem;
import tickets.factory.ConcertTicketFactory;
import tickets.factory.FlightTicketFactory;
import tickets.observer.AvailabilityBoard;
import tickets.observer.CustomerNotifier;
import tickets.observer.TicketEvent;
import tickets.state.Reservation;
import tickets.strategy.BackRowStrategy;
import tickets.strategy.FrontRowStrategy;

public final class Main {
    public static void main(String[] args) {
        TicketReservationSystem system = new TicketReservationSystem();
        CustomerNotifier customer = new CustomerNotifier();
        AvailabilityBoard board = new AvailabilityBoard();
        system.subscribe(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, customer);
        system.subscribe(TicketEvent.Type.PRICE_CHANGED, customer);
        system.subscribe(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, board);

        system.register(new EventConfiguration.Builder("CONCERT-1")
                .factory(new ConcertTicketFactory()).strategy(new FrontRowStrategy())
                .seats(Set.of(1, 2, 3)).price(new BigDecimal("50.00")).build());
        system.register(new EventConfiguration.Builder("FLIGHT-1")
                .factory(new FlightTicketFactory()).strategy(new BackRowStrategy())
                .seats(Set.of(10, 11)).price(new BigDecimal("200.00")).build());

        if (args.length > 0 && args[0].equals("--interactive")) {
            new ConsoleApp(system, System.in, System.out).run();
            return;
        }

        Reservation first = system.book("CONCERT-1", "CUSTOMER-1");
        System.out.println("Booked concert seat: " + first.ticket().seat() + " (" + first.status() + ")");
        Reservation confirmed = system.confirm(first.id());
        System.out.println("Confirmed reservation: " + confirmed.id() + " (" + confirmed.status() + ")");
        system.setStrategy("CONCERT-1", new BackRowStrategy());
        Reservation second = system.book("CONCERT-1", "CUSTOMER-2");
        System.out.println("After strategy switch, seat: " + second.ticket().seat());
        system.cancel(second.id());
        System.out.println("Cancelled reservation; concert seats available: " + system.remaining("CONCERT-1"));
        system.changePrice("CONCERT-1", new BigDecimal("60.00"));

        Reservation flight = system.book("FLIGHT-1", "CUSTOMER-3");
        System.out.println("Flight ticket: " + flight.ticket());
        System.out.println("Customer received events: " + customer.received().size());
        System.out.println("Last availability update: " + board.seatsRemaining() + " seat left");
    }
}
