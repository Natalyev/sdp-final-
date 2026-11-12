package tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tickets.builder.EventConfiguration;
import tickets.facade.TicketReservationSystem;
import tickets.factory.ConcertTicketFactory;
import tickets.strategy.FrontRowStrategy;
import tickets.web.WebApp;

class TicketWebTest {
    @Test void browserApiUsesTheRealReservationWorkflow() throws Exception {
        TicketReservationSystem system = new TicketReservationSystem();
        system.register(new EventConfiguration.Builder("CONCERT-1")
                .factory(new ConcertTicketFactory()).strategy(new FrontRowStrategy())
                .seats(Set.of(1, 2, 3)).price(new BigDecimal("50.00")).build());

        HttpClient client = HttpClient.newHttpClient();
        try (WebApp web = WebApp.start(system, 0)) {
            String base = "http://127.0.0.1:" + web.port();
            HttpResponse<String> page = get(client, base + "/");
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("Ticket Reservation System"));

            HttpResponse<String> booked = post(client, base, "action=book&event=CONCERT-1&customer=C1");
            assertEquals(200, booked.statusCode());
            assertTrue(booked.body().contains("\"seat\":1"));
            assertTrue(booked.body().contains("\"status\":\"PENDING\""));

            assertEquals(200, post(client, base, "action=confirm&id=R-1").statusCode());
            assertEquals(200, post(client, base, "action=strategy&event=CONCERT-1&policy=back").statusCode());
            assertTrue(post(client, base, "action=book&event=CONCERT-1&customer=C1")
                    .body().contains("\"seat\":3"));
            assertEquals(200, post(client, base, "action=cancel&id=R-2").statusCode());
            assertEquals(200, post(client, base, "action=price&event=CONCERT-1&price=75.00").statusCode());

            String state = get(client, base + "/api/state?customer=C1").body();
            assertTrue(state.contains("\"availableSeats\":[2,3]"));
            assertTrue(state.contains("\"price\":\"75.00\""));
            assertTrue(state.contains("\"status\":\"CONFIRMED\""));
            assertTrue(state.contains("\"status\":\"CANCELLED\""));
            assertTrue(state.contains("\"type\":\"PRICE_CHANGED\""));
        }
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String base, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + "/api/action"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
