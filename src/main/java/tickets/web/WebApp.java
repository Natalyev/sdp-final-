package tickets.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import tickets.facade.TicketReservationSystem;
import tickets.observer.TicketEvent;
import tickets.state.Reservation;
import tickets.strategy.BackRowStrategy;
import tickets.strategy.FrontRowStrategy;

/** Local browser interface over the same facade used by the console demo. */
public final class WebApp implements AutoCloseable {
    private record Activity(long id, TicketEvent event) { }

    private final TicketReservationSystem system;
    private final HttpServer server;
    private final Map<String, String> policies = new ConcurrentHashMap<>();
    private final List<Activity> activity = new ArrayList<>();
    private final AtomicLong nextActivityId = new AtomicLong();

    private WebApp(TicketReservationSystem system, int port) throws IOException {
        this.system = Objects.requireNonNull(system);
        policies.put("CONCERT-1", "front");
        policies.put("FLIGHT-1", "back");
        system.subscribe(TicketEvent.Type.SEAT_AVAILABILITY_CHANGED, this::record);
        system.subscribe(TicketEvent.Type.PRICE_CHANGED, this::record);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public static WebApp start(TicketReservationSystem system, int port) throws IOException {
        return new WebApp(system, port);
    }

    public int port() { return server.getAddress().getPort(); }

    @Override public void close() { server.stop(0); }

    private synchronized void record(TicketEvent event) {
        activity.add(new Activity(nextActivityId.incrementAndGet(), event));
        if (activity.size() > 40) activity.remove(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (method.equals("GET") && path.equals("/api/state")) {
                String customer = form(exchange.getRequestURI().getRawQuery()).getOrDefault("customer", "");
                send(exchange, 200, "application/json", state(customer).getBytes(StandardCharsets.UTF_8));
            } else if (method.equals("POST") && path.equals("/api/action")) {
                byte[] body = exchange.getRequestBody().readNBytes(8193);
                if (body.length > 8192) throw new IllegalArgumentException("Request is too large");
                String result = action(form(new String(body, StandardCharsets.UTF_8)));
                send(exchange, 200, "application/json", result.getBytes(StandardCharsets.UTF_8));
            } else if (method.equals("GET") && Set.of("/", "/index.html", "/style.css", "/app.js").contains(path)) {
                String file = path.equals("/") ? "/index.html" : path;
                String type = file.endsWith(".css") ? "text/css" : file.endsWith(".js")
                        ? "text/javascript" : "text/html";
                try (InputStream resource = WebApp.class.getResourceAsStream("/web" + file)) {
                    if (resource == null) {
                        send(exchange, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
                    } else {
                        send(exchange, 200, type, resource.readAllBytes());
                    }
                }
            } else {
                send(exchange, 404, "application/json", "{\"error\":\"Not found\"}".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException | IllegalStateException failure) {
            send(exchange, 400, "application/json", ("{\"error\":" + quote(failure.getMessage()) + "}")
                    .getBytes(StandardCharsets.UTF_8));
        } finally {
            exchange.close();
        }
    }

    private String action(Map<String, String> values) {
        String type = required(values, "action");
        Reservation reservation = null;
        switch (type) {
            case "book" -> reservation = system.book(required(values, "event"), required(values, "customer"));
            case "confirm" -> reservation = system.confirm(required(values, "id"));
            case "cancel" -> reservation = system.cancel(required(values, "id"));
            case "price" -> system.changePrice(required(values, "event"),
                    new BigDecimal(required(values, "price")));
            case "strategy" -> {
                String event = required(values, "event");
                String policy = required(values, "policy");
                if (policy.equals("front")) system.setStrategy(event, new FrontRowStrategy());
                else if (policy.equals("back")) system.setStrategy(event, new BackRowStrategy());
                else throw new IllegalArgumentException("Unknown seating strategy");
                policies.put(event, policy);
            }
            default -> throw new IllegalArgumentException("Unknown action");
        }
        return "{\"ok\":true" + (reservation == null ? "" : ",\"reservation\":" + reservation(reservation)) + "}";
    }

    private String state(String customer) {
        StringBuilder json = new StringBuilder("{\"events\":[");
        boolean first = true;
        for (String id : system.eventIds().stream().sorted().toList()) {
            if (!first) json.append(',');
            first = false;
            json.append("{\"id\":").append(quote(id))
                    .append(",\"kind\":").append(quote(id.startsWith("FLIGHT") ? "flight" : "concert"))
                    .append(",\"price\":").append(quote(system.currentPrice(id).toPlainString()))
                    .append(",\"policy\":").append(quote(policies.getOrDefault(id, "front")))
                    .append(",\"allSeats\":").append(numbers(system.allSeats(id)))
                    .append(",\"availableSeats\":").append(numbers(system.availableSeats(id)))
                    .append('}');
        }
        json.append("],\"activity\":[");
        List<Activity> recent;
        synchronized (this) { recent = List.copyOf(activity); }
        first = true;
        for (int i = recent.size() - 1; i >= 0; i--) {
            if (!first) json.append(',');
            first = false;
            Activity item = recent.get(i);
            json.append("{\"id\":").append(item.id())
                    .append(",\"type\":").append(quote(item.event().type().name()))
                    .append(",\"eventId\":").append(quote(item.event().eventId()))
                    .append(",\"seatsRemaining\":").append(item.event().seatsRemaining())
                    .append(",\"price\":").append(quote(item.event().price().toPlainString())).append('}');
        }
        json.append("],\"reservations\":[");
        first = true;
        if (!customer.isBlank()) {
            for (Reservation item : system.reservationsForCustomer(customer)) {
                if (!first) json.append(',');
                first = false;
                json.append(reservation(item));
            }
        }
        return json.append("]}").toString();
    }

    private static String reservation(Reservation item) {
        return "{\"id\":" + quote(item.id()) + ",\"customer\":" + quote(item.customerId())
                + ",\"eventId\":" + quote(item.ticket().eventId())
                + ",\"seat\":" + item.ticket().seat()
                + ",\"category\":" + quote(item.ticket().category())
                + ",\"price\":" + quote(item.ticket().price().toPlainString())
                + ",\"status\":" + quote(item.status().name()) + "}";
    }

    private static String numbers(Set<Integer> values) {
        return values.stream().sorted(Comparator.naturalOrder()).map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value.trim();
    }

    private static Map<String, String> form(String encoded) {
        Map<String, String> values = new HashMap<>();
        if (encoded == null || encoded.isEmpty()) return values;
        for (String pair : encoded.split("&")) {
            int equals = pair.indexOf('=');
            if (equals < 0) throw new IllegalArgumentException("Invalid form data");
            values.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
        }
        return values;
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') result.append("\\\"");
            else if (c == '\\') result.append("\\\\");
            else if (c == '\n') result.append("\\n");
            else if (c == '\r') result.append("\\r");
            else if (c == '\t') result.append("\\t");
            else if (c < 32) result.append(String.format("\\u%04x", (int) c));
            else result.append(c);
        }
        return result.append('"').toString();
    }

    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) { output.write(body); }
    }
}
