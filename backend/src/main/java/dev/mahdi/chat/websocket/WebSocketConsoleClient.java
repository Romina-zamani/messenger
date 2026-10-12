package dev.mahdi.chat.websocket;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.CookieManager;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.CompletionStage;

/** A small terminal client used to learn WebSocket before building React. */
public final class WebSocketConsoleClient implements WebSocket.Listener {
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private final StringBuilder frame = new StringBuilder();

  public static void main(String[] args) throws Exception {
    BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    System.out.print("Registered username: ");
    String username = keyboard.readLine();
    String password;
    if (System.console() != null) {
      char[] entered = System.console().readPassword("Password: ");
      password = new String(entered);
      java.util.Arrays.fill(entered, '\0');
    } else {
      System.out.print("Password (visible in this terminal): ");
      password = keyboard.readLine();
    }

    String endpoint = args.length == 0 ? "ws://localhost:8080/ws/chat" : args[0];
    HttpClient client = httpClient();
    URI socketUri = URI.create(endpoint);
    String base = (socketUri.getScheme().equals("wss") ? "https" : "http") + "://" + socketUri.getAuthority();
    var csrfResponse = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
    if (csrfResponse.statusCode() != 200) throw new IllegalStateException("Could not reach the login service.");
    var csrf = JSON.readTree(csrfResponse.body());
    String form = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
        + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
    var login = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header(csrf.get("headerName").asText(), csrf.get("token").asText())
        .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.discarding());
    if (login.statusCode() != 204) {
      System.err.println("Login failed. Register in the browser first, then check your username and password.");
      return;
    }
    WebSocket socket = client.newWebSocketBuilder()
        .buildAsync(URI.create(endpoint), new WebSocketConsoleClient()).join();
    System.out.println("Connected. Type a message or quit:");

    String text;
    while ((text = keyboard.readLine()) != null && !"quit".equalsIgnoreCase(text.trim())) {
      socket.sendText(toJson(text), true).join();
    }
    socket.sendClose(WebSocket.NORMAL_CLOSURE, "goodbye").join();
  }

  static String toJson(String content) {
    try { return JSON.writeValueAsString(new OutgoingMessage(content)); }
    catch (Exception error) { throw new IllegalArgumentException("Could not create JSON", error); }
  }

  private static HttpClient httpClient() throws Exception {
    String trustStorePath = System.getenv("CHAT_TRUST_STORE");
    if (trustStorePath == null || trustStorePath.isBlank()) return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    char[] password = System.getenv().getOrDefault("CHAT_TRUST_STORE_PASSWORD", "changeit").toCharArray();
    KeyStore trustStore = KeyStore.getInstance("PKCS12");
    try (InputStream input = Files.newInputStream(Path.of(trustStorePath))) { trustStore.load(input, password); }
    TrustManagerFactory managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    managers.init(trustStore);
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, managers.getTrustManagers(), null);
    return HttpClient.newBuilder().sslContext(context).cookieHandler(new CookieManager()).build();
  }

  @Override public void onOpen(WebSocket socket) { socket.request(1); }

  @Override public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
    frame.append(data);
    if (last) { System.out.println("Server event: " + frame); frame.setLength(0); }
    socket.request(1);
    return null;
  }

  private record OutgoingMessage(String content) {}
}
