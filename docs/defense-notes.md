# Defense notes - Ticket Reservation System

## Two-minute explanation

Our project is a ticket reservation system for concerts and flights. We use six design patterns in one booking flow. Builder creates a complete event configuration. The Facade gives the client one simple API. Strategy chooses a seat and can be changed at runtime. Factory Method creates a concert or flight ticket. State controls whether a reservation can be confirmed or cancelled. Observer sends seat and price changes to subscribed views. When a reservation is cancelled, its seat becomes available again.

## Live demonstration

1. Run `java -cp target/classes tickets.Main --web`, then open `http://127.0.0.1:8080`. Choose the concert, reserve the highlighted front-row seat, and confirm it in My bookings.
2. Switch to Back row first and reserve another seat. Show that the highlighted next seat jumps to the highest available number. Cancel the second booking and show the seat returning to the map and a notification appearing in Live updates.
3. Change the price, choose the flight, and point out its independent seat map and ticket factory. The browser calls the same `TicketReservationSystem` Facade as the console version.
4. Run `mvn clean test`. Show `Tests run: 20, Failures: 0, Errors: 0`.

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
| 8 | `src/main/java/tickets/web/WebApp.java` and `src/main/resources/web` | The browser sends actions to the same Facade; the page renders seats, bookings, prices, and live events. |
| 9 | `src/test/java/tickets/TicketWorkflowTest.java` and `TicketWebTest.java` | Integration tests prove the patterns work together through console and browser paths. |

## Likely questions

**Why Strategy?** The seat policy can change without rewriting the inventory or booking service.

**Why Factory Method?** Booking code requests a `Ticket` and does not need to know the concrete ticket class.

**What does State prevent?** It stops a cancelled reservation from being confirmed or cancelled again.

**How are Observer and State connected?** Cancelling changes the reservation state, returns its seat, and publishes a seat-availability event.

**What does Facade add?** It hides the coordination among service, inventory, state, factory, and publisher from the caller.

**What are the limitations?** The system is in-memory and has no payment or login. Those are future extensions. The seat map shows the next seat chosen by Strategy; customers do not manually pick a specific seat.
