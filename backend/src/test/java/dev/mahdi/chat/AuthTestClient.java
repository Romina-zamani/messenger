package dev.mahdi.chat;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;

public final class AuthTestClient {
  public final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
  private final String base;
  public AuthTestClient(int port) { base = "http://localhost:" + port; }
  public HttpResponse<String> get(String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
  }
  public HttpResponse<String> post(String path, String body, boolean json, boolean csrf) throws Exception {
    var request = HttpRequest.newBuilder(URI.create(base + path))
        .header("Content-Type", json ? "application/json" : "application/x-www-form-urlencoded");
    if (csrf) {
      var token = JsonMapper.builder().build().readTree(get("/api/auth/csrf").body());
      request.header(token.get("headerName").asText(), token.get("token").asText());
    }
    return http.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
  }
  public HttpResponse<String> register(String name) throws Exception {
    return post("/api/auth/register", "{\"username\":\"" + name + "\",\"password\":\"LearningJava123!\"}", true, true);
  }
  public HttpResponse<String> login(String name, String password) throws Exception {
    return post("/api/auth/login", "username=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
        + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8), false, true);
  }
}
