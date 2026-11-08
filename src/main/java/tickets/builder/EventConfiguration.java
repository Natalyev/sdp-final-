package tickets.builder;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import tickets.factory.TicketFactory;
import tickets.strategy.SeatSelectionStrategy;

public final class EventConfiguration {
    private final String eventId;
    private final TicketFactory factory;
    private final SeatSelectionStrategy strategy;
    private final Set<Integer> seats;
    private final BigDecimal price;

    private EventConfiguration(Builder builder) {
        eventId = builder.eventId;
        factory = Objects.requireNonNull(builder.factory, "ticket factory");
        strategy = Objects.requireNonNull(builder.strategy, "seat strategy");
        if (builder.seats == null || builder.seats.isEmpty()
                || builder.seats.stream().anyMatch(seat -> seat == null || seat < 1)) {
            throw new IllegalArgumentException("Seats must be positive and nonempty");
        }
        seats = Set.copyOf(builder.seats);
        price = Objects.requireNonNull(builder.price, "initial price");
        if (price.signum() < 0) throw new IllegalArgumentException("Price cannot be negative");
    }

    public String eventId() { return eventId; }
    public TicketFactory factory() { return factory; }
    public SeatSelectionStrategy strategy() { return strategy; }
    public Set<Integer> seats() { return seats; }
    public BigDecimal price() { return price; }

    public static final class Builder {
        private final String eventId;
        private TicketFactory factory;
        private SeatSelectionStrategy strategy;
        private Set<Integer> seats;
        private BigDecimal price;

        public Builder(String eventId) {
            if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("Event ID is required");
            this.eventId = eventId;
        }

        public Builder factory(TicketFactory value) { factory = Objects.requireNonNull(value); return this; }
        public Builder strategy(SeatSelectionStrategy value) { strategy = Objects.requireNonNull(value); return this; }
        public Builder seats(Set<Integer> value) { seats = Set.copyOf(value); return this; }
        public Builder price(BigDecimal value) { price = Objects.requireNonNull(value); return this; }
        public EventConfiguration build() { return new EventConfiguration(this); }
    }
}
