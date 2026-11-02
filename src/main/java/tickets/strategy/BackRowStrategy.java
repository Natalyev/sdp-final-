package tickets.strategy;

import java.util.Set;

public final class BackRowStrategy implements SeatSelectionStrategy {
    public int select(Set<Integer> availableSeats) {
        if (availableSeats == null || availableSeats.isEmpty()) throw new IllegalStateException("No seats available");
        return availableSeats.stream().max(Integer::compareTo).orElseThrow();
    }
}
