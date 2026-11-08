package tickets.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;
import tickets.facade.TicketReservationSystem;
import tickets.observer.TicketEvent;
import tickets.state.Reservation;
import tickets.strategy.BackRowStrategy;
import tickets.strategy.FrontRowStrategy;

public final class ConsoleApp {
    private final TicketReservationSystem system;
    private final Scanner input;
    private final PrintStream output;

    public ConsoleApp(TicketReservationSystem system, InputStream input, PrintStream output) {
        this.system = Objects.requireNonNull(system);
        this.input = new Scanner(Objects.requireNonNull(input));
        this.output = Objects.requireNonNull(output);
    }

    public void run() {
        system.subscribe(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, this::printEvent);
        system.subscribe(TicketEvent.Type.PRICE_CHANGED, this::printEvent);
        output.println("Ticket Reservation System. Type 'help' for commands.");
        while (true) {
            output.print("> ");
            if (!input.hasNextLine()) return;
            String line = input.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            try {
                if (execute(parts)) return;
            } catch (IllegalArgumentException | IllegalStateException failure) {
                output.println("ERROR: " + failure.getMessage());
            }
        }
    }

    private boolean execute(String[] parts) {
        switch (parts[0].toLowerCase()) {
            case "help" -> {
                require(parts, 1);
                output.println("events | book EVENT CUSTOMER | confirm ID | cancel ID | show ID");
                output.println("price EVENT AMOUNT | strategy EVENT front|back | quit");
            }
            case "events" -> {
                require(parts, 1);
                system.eventIds().stream().sorted().forEach(id ->
                        output.println(id + ": " + system.remaining(id) + " seats available"));
            }
            case "book" -> {
                require(parts, 3);
                printReservation(system.book(parts[1], parts[2]));
            }
            case "confirm" -> {
                require(parts, 2);
                printReservation(system.confirm(parts[1]));
            }
            case "cancel" -> {
                require(parts, 2);
                printReservation(system.cancel(parts[1]));
            }
            case "show" -> {
                require(parts, 2);
                printReservation(system.reservation(parts[1]));
            }
            case "price" -> {
                require(parts, 3);
                system.changePrice(parts[1], new BigDecimal(parts[2]));
                output.println("Price updated for " + parts[1]);
            }
            case "strategy" -> {
                require(parts, 3);
                if (parts[2].equalsIgnoreCase("front")) system.setStrategy(parts[1], new FrontRowStrategy());
                else if (parts[2].equalsIgnoreCase("back")) system.setStrategy(parts[1], new BackRowStrategy());
                else throw new IllegalArgumentException("Strategy must be front or back");
                output.println("Strategy updated for " + parts[1]);
            }
            case "quit", "exit" -> {
                require(parts, 1);
                output.println("Goodbye.");
                return true;
            }
            default -> output.println("Unknown command. Type 'help'.");
        }
        return false;
    }

    private void printReservation(Reservation reservation) {
        output.println(reservation.id() + " " + reservation.status() + " " + reservation.ticket());
    }

    private void printEvent(TicketEvent event) {
        output.println("EVENT " + event.type() + " " + event.eventId()
                + " seats=" + event.seatsRemaining() + " price=" + event.price());
    }

    private static void require(String[] parts, int expected) {
        if (parts.length != expected) throw new IllegalArgumentException("Expected " + expected + " words: " + Arrays.toString(parts));
    }
}
