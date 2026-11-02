package tickets.observer;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TicketEventPublisher implements Subject {
    private final ConcurrentHashMap<TicketEvent.Type, CopyOnWriteArrayList<Observer>> listeners = new ConcurrentHashMap<>();

    public void subscribe(TicketEvent.Type type, Observer observer) {
        listeners.computeIfAbsent(Objects.requireNonNull(type), ignored -> new CopyOnWriteArrayList<>())
                .addIfAbsent(Objects.requireNonNull(observer));
    }

    public void unsubscribe(TicketEvent.Type type, Observer observer) {
        var group = listeners.get(Objects.requireNonNull(type));
        if (group != null) group.remove(Objects.requireNonNull(observer));
    }

    public void publish(TicketEvent event) {
        Objects.requireNonNull(event);
        var group = listeners.get(event.type());
        if (group != null) for (Observer observer : group) observer.onEvent(event);
    }
}
