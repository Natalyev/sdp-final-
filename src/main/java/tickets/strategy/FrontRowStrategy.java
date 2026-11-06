package tickets.strategy;

import java.util.Set;

public final class FrontRowStrategy implements SeatSelectionStrategy {
    public int select(Set<Integer> availableSeats) {
        if (availableSeats == null || availableSeats.isEmpty()) throw new IllegalStateException("No seats available");
        return availableSeats.stream().min(Integer::compareTo).orElseThrow();
    }
}
