package tickets.strategy;

import java.util.Set;

public interface SeatSelectionStrategy {
    int select(Set<Integer> availableSeats);
}
