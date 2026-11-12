# Ticket Reservation System

Java 17 ticket reservation application integrating six design patterns. It supports concert and flight events, booking, confirmation, cancellation, price changes, seat-policy changes, and customer notifications. A responsive browser interface is available for live demonstrations.

## Run

Open this folder as a Maven project in VS Code. Run `tickets.Main` for the original console demo. For the browser interface, run:

```text
mvn package
java -cp target/classes tickets.Main --web
```

Open `http://127.0.0.1:8080` in a browser. The server listens only on your computer. To use a different port, add it after `--web`, for example `--web 8090`. Stop the server with Ctrl+C. The browser demo starts with 30 concert seats and 18 flight seats. You can book, confirm or cancel reservations, switch the seating policy, update prices, and watch the seat map and notifications change immediately. Data resets when the server restarts.

To run the original console demo and tests:

```text
mvn test
mvn exec:java
```

The demo books and confirms a concert seat, switches the seat-selection policy, books and cancels another seat, changes the concert price, and books a flight seat.

For an interactive terminal session, run `java -cp target/classes tickets.Main --interactive` after `mvn package`. Available commands: `help`, `events`, `book EVENT CUSTOMER`, `confirm ID`, `cancel ID`, `show ID`, `price EVENT AMOUNT`, `strategy EVENT front|back`, and `quit`. The sample events are `CONCERT-1` and `FLIGHT-1`.

## Six integrated patterns

| Pattern | Classes | Purpose in the workflow |
| --- | --- | --- |
| Factory Method | `TicketFactory`, `ConcertTicketFactory`, `FlightTicketFactory` | Create the right ticket type when a seat is booked. |
| Strategy | `SeatSelectionStrategy`, `FrontRowStrategy`, `BackRowStrategy`, `SeatInventory` | Choose the lowest or highest available seat; the policy can change at runtime. |
| Observer | `Subject`, `Observer`, `TicketEventPublisher`, `CustomerNotifier`, `AvailabilityBoard` | Notify listeners after a seat count or price changes. |
| Builder | `EventConfiguration.Builder` | Assemble and validate an event's factory, seating strategy, seat set, and initial price before registration. |
| State | `ReservationState`, `PendingState`, `ConfirmedState`, `CancelledState`, `ReservationSession` | Define which confirmation and cancellation transitions are allowed. |
| Facade | `TicketReservationSystem` | Offer one API for registering events, booking, confirming, cancelling, changing price/policy, and subscribing to notifications. |

The facade creates a `ReservationService` and `SeatInventory` for each registered event. On booking, the service asks the current Strategy for a seat, uses the configured Factory to create a ticket, and publishes an Observer event. The State objects control the reservation lifecycle. Cancellation returns the seat and publishes another availability event. The Builder supplies each event's initial configuration.

## Example

```java
TicketReservationSystem system = new TicketReservationSystem();
system.register(new EventConfiguration.Builder("SHOW")
        .factory(new ConcertTicketFactory())
        .strategy(new FrontRowStrategy())
        .seats(Set.of(1, 2, 3))
        .price(new BigDecimal("50.00"))
        .build());
Reservation pending = system.book("SHOW", "CUSTOMER-1");
Reservation confirmed = system.confirm(pending.id());
```

`Reservation` is an immutable snapshot. To change its state, call the facade with its ID. A cancelled reservation cannot be confirmed or cancelled again. A released seat can be booked again.

## Project files

- `src/main/java/tickets/Main.java`: runnable demonstration.
- `src/main/java/tickets/cli/ConsoleApp.java`: interactive terminal interface.
- `src/main/java/tickets/web/WebApp.java`: local HTTP server and browser API.
- `src/main/resources/web`: responsive browser interface.
- `src/main/java/tickets/facade`: public workflow API.
- `src/main/java/tickets/builder`: validated event setup.
- `src/main/java/tickets/state`: reservation lifecycle.
- `src/main/java/tickets/factory`, `strategy`, `observer`, `service`, `model`: core behavior.
- `src/test/java/tickets`: unit and integration tests.
- `docs/class-diagram.puml`: editable UML diagram.
- `report/FinalReport.md`: editable report source.
- `output/pdf/FinalReport.pdf`: ready-to-read report.
- `docs/defense-notes.md`: short defense guide.

## Verification

Twenty JUnit tests cover factories, seat strategies, subscriptions, booking flow, state transitions, cancellation, independent event inventories, interactive commands, customer reservation lookup, browser API integration, and recovery from ticket-creation failure. `mvn clean test` passes. The project is intentionally in-memory; it does not include payments or persistent storage.
