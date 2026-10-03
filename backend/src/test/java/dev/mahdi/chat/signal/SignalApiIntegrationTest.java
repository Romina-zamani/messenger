package dev.mahdi.chat.signal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SignalApiIntegrationTest {
  @LocalServerPort int port;
  @Autowired ObjectMapper json;

  @Test void snapshotEndpointReturnsPersistedSignals() throws Exception {
    HttpResponse<String> response = HttpClient.newHttpClient().send(
      HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/signals"))
        .header("Origin", "http://127.0.0.1:5173").GET().build(),
      HttpResponse.BodyHandlers.ofString());
    SignalView[] signals = json.readValue(response.body(), SignalView[].class);
    assertEquals(200, response.statusCode());
    assertEquals(1000, signals.length);
    assertNotNull(signals[0].id());
    assertFalse(signals[0].name().isBlank());
    assertEquals("http://127.0.0.1:5173", response.headers().firstValue("access-control-allow-origin").orElse(null));
  }
}
