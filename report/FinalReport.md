# Ticket Reservation System

**Software Design Patterns - Final Project Report**
**Team:** Natalyev, eslishnah, MangoDeath
**Repository:** https://github.com/Natalyev/sdp-final-
**Technology:** Java 17, Maven, JUnit 5

## 1. Abstract

Ticket Reservation System is an in-memory Java application for concert and flight bookings. A user registers an event, books a numbered seat, confirms or cancels the reservation, changes the ticket price, and switches the seat-selection policy. Customer and availability views receive notifications when prices or seat counts change. A runnable demo and an interactive command-line mode show the full workflow. The implementation integrates six design patterns: Factory Method, Strategy, Observer, Builder, State, and Facade.

This introductory section defines the problem: booking logic must choose seats, create different tickets, control reservation changes, and notify customers without placing every responsibility in one class.

## 2. Requirements and user scenario

The system supports multiple event categories and independent seat inventories. A booking chooses an available seat according to the active policy and creates the correct ticket type. A new booking starts as pending. Confirmation marks it confirmed; cancellation marks it cancelled and returns its seat. The system rejects invalid transitions, duplicate event IDs, unknown IDs, and booking when no seats remain. Price changes affect future tickets, while tickets already created keep their original price. Seat and price updates are published to subscribed listeners.

Example: register `CONCERT-1` with seats 1, 2, 3 and a front-row policy. A customer books seat 1 and confirms it. Change the policy to back-row; the next customer books seat 3. Cancel that booking; seat 3 becomes available again. Update the concert price; the next booking uses the new price. A flight event has its own seats, factory, and price.

## 3. Architecture

`TicketReservationSystem` is the facade used by clients. For each registered event, it owns one `ReservationService` and one `SeatInventory`. `EventConfiguration.Builder` creates a valid configuration before registration. `ReservationService` asks `SeatInventory` to choose a seat and uses a `TicketFactory` to create a concrete ticket. It publishes seat and price events through `TicketEventPublisher`. The facade stores a `ReservationSession` for each booking. The session delegates transitions to State objects and returns immutable `Reservation` snapshots.

The editable UML diagram is `docs/class-diagram.puml`. The `Main` class demonstrates the default scenario. `ConsoleApp` provides the interactive terminal interface.

## 4. Pattern implementation and integration

| Pattern | Main participants | Role and integration |
| --- | --- | --- |
| Factory Method | `TicketFactory`, `ConcertTicketFactory`, `FlightTicketFactory` | The service calls the configured factory when booking. Concert and flight tickets share the `Ticket` interface but are separate concrete records. |
| Strategy | `SeatSelectionStrategy`, `FrontRowStrategy`, `BackRowStrategy`, `SeatInventory` | The inventory delegates seat choice to a policy. The facade can replace the policy while the program runs without changing booking code. |
| Observer | `Subject`, `Observer`, `TicketEventPublisher`, `CustomerNotifier`, `AvailabilityBoard` | The service publishes seat-availability and price events. Subscribers receive only the types they requested. The availability board stores counts by event ID. |
| Builder | `EventConfiguration.Builder` | The client assembles ticket factory, seating policy, seat numbers, and price. `build()` rejects incomplete or invalid configurations. The facade accepts only a complete configuration. |
| State | `ReservationState`, `PendingState`, `ConfirmedState`, `CancelledState`, `ReservationSession` | Pending can be confirmed or cancelled. Confirmed can remain confirmed or be cancelled. Cancelled rejects further transitions. The facade returns immutable snapshots so callers cannot bypass seat release. |
| Facade | `TicketReservationSystem` | Provides one API for registration, booking, confirmation, cancellation, price and policy changes, subscriptions, and a customer's reservation history. It coordinates the other five patterns. |

The six patterns form one chain: **Builder -> Facade -> Strategy -> Factory Method -> State**, with **Observer** notifying clients after seat and price changes.

### Java code example

The client configures an event with Builder and then uses the Facade. The Facade coordinates Strategy, Factory Method, State, and Observer internally.

```java
TicketReservationSystem system = new TicketReservationSystem();
system.register(new EventConfiguration.Builder("SHOW")
        .factory(new ConcertTicketFactory())
        .strategy(new FrontRowStrategy())
        .seats(Set.of(1, 2, 3))
        .price(new BigDecimal("50.00"))
        .build());
Reservation booking = system.book("SHOW", "CUSTOMER-1");
system.confirm(booking.id());
```

## 5. Important design decisions

- Each event has a separate inventory and service, so concert and flight seats cannot interfere.
- `SeatInventory` removes a chosen seat atomically and rejects a strategy that selects an unavailable seat. Cancelling a reservation releases the seat once.
- `Reservation` is an immutable snapshot. The facade owns mutable sessions and handles all transitions that affect seats.
- Ticket creation failure releases the selected seat before propagating the error.
- Listeners are stored in `CopyOnWriteArrayList`, which permits subscription changes while notifications are being delivered.
- Notifications are synchronous. A listener sees a complete `TicketEvent` with event ID, seat count, and price.

## 6. Verification and evidence

Run `mvn clean test`: **19 tests pass, 0 failures, 0 errors**. Ten tests check factories, seat strategies, Observer subscriptions, and basic service integration. Eight workflow tests check Builder validation, all-pattern integration, cancellation and seat reuse, independent event inventories, invalid IDs, interactive commands, ticket-creation rollback, and per-event availability counts. One Facade test checks customer reservation lookup.

Run `tickets.Main` to see the deterministic demo. Run `tickets.Main --interactive` to enter commands. Verified terminal transcripts are included in `evidence/test-run.txt`, `evidence/demo-run.txt`, and `evidence/interactive-run.txt`.

## 7. Running the project

Requirements: JDK 17 or newer and Maven. In VS Code, open the project folder, run `tickets.Main`, and run the JUnit tests from the Testing panel. In a terminal:

```text
mvn clean test
mvn exec:java
mvn package
java -cp target/classes tickets.Main --interactive
```

Interactive commands: `help`, `events`, `book EVENT CUSTOMER`, `confirm ID`, `cancel ID`, `show ID`, `price EVENT AMOUNT`, `strategy EVENT front|back`, and `quit`.

## 8. Limitations and future work

Data is held in memory and is lost when the process ends. The project has no payment processing, account authentication, database, seat map, or multi-seat orders. These are possible extensions, but they are not required to demonstrate the six integrated patterns. The command-line interface is sufficient for the current workflow and defense.

## 9. Execution evidence

The demo and interactive transcripts in `evidence/` come from actual runs of the current code. The demo shows seat 1 booked with the front-row strategy, seat 3 booked after switching to back-row, and seat 3 returned by cancellation. The interactive run shows the corresponding availability events and a restored seat count. `evidence/test-run.txt` contains the Maven test result.

## 10. Team and repository

The repository records contributions under the GitHub handles **Natalyev**, **eslishnah**, and **MangoDeath**. The commit history is the source for exact file-level attribution. The team should verify its final contribution statement and provide full names if the course submission form requires them.

## 11. Conclusion

The working prototype demonstrates the requested Ticket Reservation System topic. Six patterns participate in the same booking workflow: event setup, seat selection, ticket creation, reservation state changes, and notifications. The Maven build succeeds and all 19 tests pass.

## 12. Further work

Persistent storage, payment processing, multi-seat orders, authentication, and a graphical seat map would make the prototype suitable for broader use. These additions would build on the existing Facade and event-specific services.
