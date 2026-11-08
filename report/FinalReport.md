# Ticket Reservation System - Final Report Draft

## 1. Project overview

The application manages reservations for concert and flight events. A user can register an event with its seats, starting price, ticket type, and seat-selection policy. Customers can book a seat, confirm or cancel a reservation, and receive notifications when seats or prices change. The runnable command-line demo is `tickets.Main`.

## 2. Problem and goal

Ticket systems need to support different event types and booking rules without placing all decisions in one large class. The goal is to keep ticket creation, seat selection, notifications, event setup, reservation lifecycle, and the public workflow separate while making them work together in one reservation flow.

## 3. Architecture and pattern roles

| Pattern | Participants | Why it is used |
| --- | --- | --- |
| Factory Method | `TicketFactory` and its concert/flight implementations | `ReservationService` requests a `Ticket` without knowing which concrete type it will receive. |
| Strategy | `SeatSelectionStrategy`, `FrontRowStrategy`, `BackRowStrategy`, `SeatInventory` | The seat policy is replaceable while the program runs. |
| Observer | `Subject`, `Observer`, `TicketEventPublisher`, `CustomerNotifier`, `AvailabilityBoard` | A price or seat update is published to subscribed views without hard-coding those views into the service. |
| Builder | `EventConfiguration.Builder` | Registration requires a complete, valid combination of seats, ticket factory, strategy, and price. |
| State | `ReservationState` and its pending/confirmed/cancelled implementations | Allowed actions depend on the current reservation status. A cancelled reservation cannot be confirmed or cancelled again. |
| Facade | `TicketReservationSystem` | Client code uses one entry point for booking, confirmation, cancellation, price updates, strategy changes, and subscriptions. |

The UML source is in `docs/class-diagram.puml`.

## 4. Main scenario

1. Build an `EventConfiguration` and register it with the facade.
2. The facade creates a separate `SeatInventory` and `ReservationService` for that event.
3. `book` asks the inventory's current Strategy to choose a seat, then the configured Factory creates a ticket.
4. The service publishes a seat-availability event; customer and board observers receive it if subscribed.
5. The booking starts in `PENDING`. `confirm` changes it to `CONFIRMED` through a State object.
6. `cancel` changes it to `CANCELLED`, releases its seat, and publishes another seat-availability event.
7. `changePrice` publishes a price event. The next ticket uses the new price; already created tickets retain their original price.

## 5. Design decisions and limitations

- Each event has its own inventory and service. The event ID identifies them in the facade.
- The facade returns immutable `Reservation` snapshots. All state changes go through the facade so that cancellation also returns the seat.
- The seat strategies select from a copy of the currently available seats. The inventory rejects an unavailable choice.
- Event delivery is synchronous. `CopyOnWriteArrayList` allows a listener to subscribe or unsubscribe during notification.
- Data is held in memory. Closing the program loses reservations; persistence and a graphical or web interface are future work.
- The current model supports one price per event and numbered seats. Payments, seat maps, and multi-seat orders are outside this stage.

## 6. Verification

Run `mvn test` to execute 15 JUnit 5 tests. The original ten tests cover the first three patterns. Five workflow tests cover Builder validation; integration of all six patterns; state transitions; cancellation and reuse of seats; separate event inventories and ticket factories; and invalid IDs. Run `tickets.Main` to show the complete scenario in the terminal.

## 7. Material still needed for final submission

- Confirm the exact defense rubric and add required screenshots of the running demo and passing tests.
- Render the UML source as an image and include it in the final report.
- Convert this draft into the required PDF report and add title page, team roster, contributions verified by each member, and references if required.
- Prepare the source ZIP and a short team defense presentation or speaking notes.
- Perform a final review of the repository history and submit the required artifacts.
