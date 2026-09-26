package dev.mahdi.chat.lesson2;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.*;

public final class ChatServer implements AutoCloseable {
  private final ServerSocket socket;
  private final ExecutorService workers = Executors.newCachedThreadPool();
  private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
  private volatile boolean running;

  public ChatServer(int port) throws IOException { socket = new ServerSocket(port); }
  public int port() { return socket.getLocalPort(); }

  public void start() {
    running = true;
    workers.submit(() -> {
      System.out.println("Multi-client server listening on " + port());
      while (running) {
        try { workers.submit(new ClientHandler(socket.accept())); }
        catch (SocketException stopped) { if (running) stopped.printStackTrace(); }
        catch (IOException error) { if (running) error.printStackTrace(); }
      }
    });
  }

  private void broadcast(String message) {
    clients.forEach(client -> client.send(message));
  }

  @Override public void close() throws IOException {
    running = false;
    socket.close();
    clients.forEach(ClientHandler::closeSilently);
    workers.shutdownNow();
  }

  private final class ClientHandler implements Runnable {
    private final Socket client; private PrintWriter output; private String nickname;
    ClientHandler(Socket client) { this.client = client; }
    public void run() {
      try (client; BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
        output = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8);
        nickname = input.readLine();
        if (nickname == null || nickname.isBlank()) return;
        clients.add(this);
        broadcast("SYSTEM: " + nickname + " joined");
        String text;
        while ((text = input.readLine()) != null && !"quit".equalsIgnoreCase(text.trim())) {
          if (!text.isBlank()) broadcast(nickname + ": " + text);
        }
      } catch (IOException ignored) {
        // A browser or terminal may disappear without saying quit.
      } finally {
        if (clients.remove(this) && nickname != null) broadcast("SYSTEM: " + nickname + " left");
      }
    }
    void send(String message) { if (output != null) output.println(message); }
    void closeSilently() { try { client.close(); } catch (IOException ignored) {} }
  }

  public static void main(String[] args) throws Exception {
    ChatServer server = new ChatServer(5000); server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { server.close(); } catch (IOException ignored) {} }));
    new CountDownLatch(1).await();
  }
}

