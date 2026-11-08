# Defense notes - Ticket Reservation System

## Two-minute explanation

Our project is a ticket reservation system for concerts and flights. We use six design patterns in one booking flow. Builder creates a complete event configuration. The Facade gives the client one simple API. Strategy chooses a seat and can be changed at runtime. Factory Method creates a concert or flight ticket. State controls whether a reservation can be confirmed or cancelled. Observer sends seat and price changes to subscribed views. When a reservation is cancelled, its seat becomes available again.

## Live demonstration

1. Run `tickets.Main`. Point out the first concert booking in `PENDING`, its confirmation, the switch from front-row to back-row, cancellation, and the flight ticket.
2. Run `tickets.Main --interactive`. Enter `events`, `book CONCERT-1 C1`, `confirm R-1`, `cancel R-1`, `events`, `quit`.
3. Run `mvn clean test`. Show `Tests run: 18, Failures: 0, Errors: 0`.

## Files to show

| Order | File | What to say |
| --- | --- | --- |
| 1 | `src/main/java/tickets/Main.java` | Registers two example events and runs the scenario. |
| 2 | `src/main/java/tickets/builder/EventConfiguration.java` | Builder validates the setup before registration. |
| 3 | `src/main/java/tickets/facade/TicketReservationSystem.java` | Facade coordinates booking, confirmation, cancellation, and notifications. |
| 4 | `src/main/java/tickets/strategy/SeatSelectionStrategy.java` and implementations | Strategy chooses the smallest or largest available seat. |
| 5 | `src/main/java/tickets/factory/TicketFactory.java` and implementations | Factory creates the right concrete ticket type. |
| 6 | `src/main/java/tickets/state/ReservationState.java` | State classes decide which transitions are allowed. |
| 7 | `src/main/java/tickets/observer/TicketEventPublisher.java` | Observer dispatches events to subscribed listeners. |
| 8 | `src/test/java/tickets/TicketWorkflowTest.java` | Integration tests prove the patterns work together. |

## Likely questions

**Why Strategy?** The seat policy can change without rewriting the inventory or booking service.

**Why Factory Method?** Booking code requests a `Ticket` and does not need to know the concrete ticket class.

**What does State prevent?** It stops a cancelled reservation from being confirmed or cancelled again.

**How are Observer and State connected?** Cancelling changes the reservation state, returns its seat, and publishes a seat-availability event.

**What does Facade add?** It hides the coordination among service, inventory, state, factory, and publisher from the caller.

**What are the limitations?** The system is in-memory and has no payment or login. Those are future extensions.
