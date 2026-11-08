package tickets.facade;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tickets.builder.EventConfiguration;
import tickets.observer.Observer;
import tickets.observer.TicketEvent;
import tickets.observer.TicketEventPublisher;
import tickets.model.Ticket;
import tickets.service.ReservationService;
import tickets.service.SeatInventory;
import tickets.state.Reservation;
import tickets.state.ReservationSession;
import tickets.strategy.SeatSelectionStrategy;

public final class TicketReservationSystem {
    private record EventContext(ReservationService service, SeatInventory inventory) { }

    private final TicketEventPublisher events = new TicketEventPublisher();
    private final Map<String, EventContext> eventContexts = new HashMap<>();
    private final Map<String, ReservationSession> reservations = new LinkedHashMap<>();
    private long nextReservationId = 1;

    public synchronized void register(EventConfiguration configuration) {
        Objects.requireNonNull(configuration);
        if (eventContexts.containsKey(configuration.eventId()))
            throw new IllegalArgumentException("Event already registered: " + configuration.eventId());
        SeatInventory inventory = new SeatInventory(configuration.seats(), configuration.strategy());
        ReservationService service = new ReservationService(configuration.factory(), inventory, events, configuration.price());
        eventContexts.put(configuration.eventId(), new EventContext(service, inventory));
    }

    public synchronized Reservation book(String eventId, String customerId) {
        if (customerId == null || customerId.isBlank()) throw new IllegalArgumentException("Customer ID is required");
        ReservationService service = context(eventId).service();
        Ticket ticket = service.reserve(eventId);
        ReservationSession session = new ReservationSession("R-" + nextReservationId++, customerId, ticket);
        Reservation reservation = session.snapshot();
        reservations.put(reservation.id(), session);
        return reservation;
    }

    public synchronized Reservation confirm(String reservationId) {
        ReservationSession session = session(reservationId);
        session.confirm();
        return session.snapshot();
    }

    public synchronized Reservation cancel(String reservationId) {
        ReservationSession session = session(reservationId);
        session.cancel();
        Reservation reservation = session.snapshot();
        context(reservation.ticket().eventId()).service().release(reservation.ticket());
        return reservation;
    }

    public synchronized Reservation reservation(String reservationId) {
        return session(reservationId).snapshot();
    }

    public synchronized List<Reservation> reservationsForCustomer(String customerId) {
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("Customer ID is required");
        return reservations.values().stream()
                .map(ReservationSession::snapshot)
                .filter(reservation -> reservation.customerId().equals(customerId))
                .toList();
    }

    public synchronized int remaining(String eventId) { return context(eventId).inventory().remaining(); }
    public synchronized Set<String> eventIds() { return Set.copyOf(eventContexts.keySet()); }
    public synchronized void changePrice(String eventId, BigDecimal price) { context(eventId).service().changePrice(eventId, price); }
    public synchronized void setStrategy(String eventId, SeatSelectionStrategy strategy) { context(eventId).inventory().setStrategy(strategy); }
    public void subscribe(TicketEvent.Type type, Observer observer) { events.subscribe(type, observer); }
    public void unsubscribe(TicketEvent.Type type, Observer observer) { events.unsubscribe(type, observer); }

    private EventContext context(String eventId) {
        EventContext context = eventContexts.get(eventId);
        if (context == null) throw new IllegalArgumentException("Unknown event: " + eventId);
        return context;
    }

    private ReservationSession session(String reservationId) {
        ReservationSession session = reservations.get(reservationId);
        if (session == null) throw new IllegalArgumentException("Unknown reservation: " + reservationId);
        return session;
    }
}
