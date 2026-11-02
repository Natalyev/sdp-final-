package tickets.observer;

public final class AvailabilityBoard implements Observer {
    private volatile int seatsRemaining;
    public void onEvent(TicketEvent event) { seatsRemaining = event.seatsRemaining(); }
    public int seatsRemaining() { return seatsRemaining; }
}
