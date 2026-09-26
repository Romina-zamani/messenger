package dev.mahdi.chat.lesson2;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class ChatServerTest {
  @Test void broadcastsOneClientsMessageToAllClients() {
    assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
      try (ChatServer server = new ChatServer(0)) {
        server.start();
        try (Peer alice = new Peer(server.port(), "Alice")) {
          assertEquals("SYSTEM: Alice joined", alice.read());
          try (Peer bob = new Peer(server.port(), "Bob")) {
            assertEquals("SYSTEM: Bob joined", alice.read());
            assertEquals("SYSTEM: Bob joined", bob.read());
            alice.send("hello everyone");
            assertEquals("Alice: hello everyone", alice.read());
            assertEquals("Alice: hello everyone", bob.read());
          }
        }
      }
    });
  }

  private static final class Peer implements AutoCloseable {
    private final Socket socket; private final BufferedReader in; private final PrintWriter out;
    Peer(int port, String nickname) throws IOException {
      socket = new Socket("localhost", port);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
      out.println(nickname);
    }
    void send(String text) { out.println(text); }
    String read() throws IOException { return in.readLine(); }
    public void close() throws IOException { socket.close(); }
  }
}
