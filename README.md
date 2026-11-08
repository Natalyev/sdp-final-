# Ticket Reservation System

Java 17 command-line demonstration of a ticket reservation workflow. The current version integrates six design patterns and is intended as an approximately 80–90% stage of the final project. It supports concert and flight events, booking, confirmation, cancellation, price changes, seat-policy changes, and customer notifications.

## Run

Open this folder as a Maven project in VS Code. Run `tickets.Main` for the demo and the two test classes from the Testing panel. With Maven installed:

```text
mvn test
mvn exec:java
```

The demo books and confirms a concert seat, switches the seat-selection policy, books and cancels another seat, changes the concert price, and books a flight seat.

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
- `src/main/java/tickets/facade`: public workflow API.
- `src/main/java/tickets/builder`: validated event setup.
- `src/main/java/tickets/state`: reservation lifecycle.
- `src/main/java/tickets/factory`, `strategy`, `observer`, `service`, `model`: core behavior.
- `src/test/java/tickets`: unit and integration tests.
- `docs/class-diagram.puml`: editable UML diagram.
- `report/FinalReport.md`: draft defense report and remaining submission work.

## Current verification and remaining work

Fifteen JUnit tests cover factories, seat strategies, subscriptions, booking flow, state transitions, cancellation, and independent event inventories. `mvn test` passes. Before final submission, the team still needs to produce the required PDF report, screenshots, source ZIP, and presentation material, then review the final rubric and team contribution details.
