package tickets.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CustomerNotifier implements Observer {
    private final CopyOnWriteArrayList<TicketEvent> received = new CopyOnWriteArrayList<>();
    public void onEvent(TicketEvent event) { received.add(event); }
    public List<TicketEvent> received() { return List.copyOf(received); }
}
