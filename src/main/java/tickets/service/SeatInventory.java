package tickets.service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import tickets.strategy.SeatSelectionStrategy;

public final class SeatInventory {
    private final Set<Integer> availableSeats;
    private SeatSelectionStrategy strategy;

    public SeatInventory(Set<Integer> seats, SeatSelectionStrategy strategy) {
        if (seats == null || seats.isEmpty() || seats.stream().anyMatch(n -> n == null || n < 1))
            throw new IllegalArgumentException("Seats must be positive and nonempty");
        this.availableSeats = new HashSet<>(seats);
        setStrategy(strategy);
    }

    public synchronized void setStrategy(SeatSelectionStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy);
    }

    public synchronized int reserveSeat() {
        int chosen = strategy.select(Set.copyOf(availableSeats));
        if (!availableSeats.remove(chosen)) throw new IllegalStateException("Strategy selected an unavailable seat");
        return chosen;
    }

    public synchronized int remaining() { return availableSeats.size(); }
}
