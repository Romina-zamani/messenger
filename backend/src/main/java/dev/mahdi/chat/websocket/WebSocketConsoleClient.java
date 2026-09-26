package dev.mahdi.chat.websocket;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

/** A small terminal client used to learn WebSocket before building React. */
public final class WebSocketConsoleClient implements WebSocket.Listener {
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private final StringBuilder frame = new StringBuilder();

  public static void main(String[] args) throws Exception {
    BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    System.out.print("Nickname: ");
    String nickname = keyboard.readLine();

    WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
        .buildAsync(URI.create("ws://localhost:8080/ws/chat"), new WebSocketConsoleClient()).join();
    System.out.println("Connected. Type a message or quit:");

    String text;
    while ((text = keyboard.readLine()) != null && !"quit".equalsIgnoreCase(text.trim())) {
      socket.sendText(toJson(nickname, text), true).join();
    }
    socket.sendClose(WebSocket.NORMAL_CLOSURE, "goodbye").join();
  }

  static String toJson(String nickname, String content) {
    try { return JSON.writeValueAsString(new OutgoingMessage(nickname, content)); }
    catch (Exception error) { throw new IllegalArgumentException("Could not create JSON", error); }
  }

  @Override public void onOpen(WebSocket socket) { socket.request(1); }

  @Override public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
    frame.append(data);
    if (last) { System.out.println("Server event: " + frame); frame.setLength(0); }
    socket.request(1);
    return null;
  }

  private record OutgoingMessage(String nickname, String content) {}
}
