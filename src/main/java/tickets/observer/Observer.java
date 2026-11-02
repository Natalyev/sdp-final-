package tickets.observer;

@FunctionalInterface
public interface Observer {
    void onEvent(TicketEvent event);
}
