# Ticket Reservation System — in progress

This is an early, roughly 40% stage of the final project. It implements a working core with **Factory Method**, **Strategy**, and **Observer**. It is not yet the six-pattern final submission.

## What works now

- `ConcertTicketFactory` and `FlightTicketFactory` create different ticket types.
- `SeatInventory` delegates seat choice to `FrontRowStrategy` or `BackRowStrategy`; the policy can change at runtime.
- `ReservationService` reserves seats, changes prices, and publishes availability and price events.
- `CustomerNotifier` and `AvailabilityBoard` subscribe to the events they need. They can unsubscribe while the program runs.
- `Main` demonstrates both ticket types, both seating policies, and event delivery.
- Ten JUnit 5 tests cover the three patterns and their integration.

## Run

Open this folder in VS Code as a Maven project. Run `tickets.Main` for the demo, or run `TicketPatternsTest` from the Testing panel. From a terminal with Maven available:

```text
mvn test
mvn exec:java
```

## Current structure

`factory` creates tickets; `strategy` chooses available seats; `observer` delivers events; `service` coordinates reservations; `model` contains ticket data. The current class diagram is in `docs/class-diagram.puml`.

## Remaining work for the final submission

The course requires at least six coordinated patterns, a cohesive application, a PDF report, screenshots, UML, a ZIP of source code, and a team defense. The next stage should add three meaningful patterns around the same reservation flow (for example, Builder for reservation options, Facade for checkout, and Visitor for reports), then complete the interface, final tests, screenshots, and report. Those items are intentionally unfinished at this stage.
