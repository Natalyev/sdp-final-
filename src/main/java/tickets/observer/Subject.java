package tickets.observer;

public interface Subject {
    void subscribe(TicketEvent.Type type, Observer observer);
    void unsubscribe(TicketEvent.Type type, Observer observer);
    void publish(TicketEvent event);
}
