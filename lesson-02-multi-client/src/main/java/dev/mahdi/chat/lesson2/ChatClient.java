package dev.mahdi.chat.lesson2;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class ChatClient {
  public static void main(String[] args) throws Exception {
    BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    System.out.print("Nickname: "); String nickname = keyboard.readLine();
    try (Socket socket = new Socket("localhost", 5000);
         BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
         PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
      output.println(nickname);
      Thread reader = Thread.ofVirtual().start(() -> {
        try { String line; while ((line = input.readLine()) != null) System.out.println(line); }
        catch (IOException ignored) { System.out.println("Disconnected."); }
      });
      String line; while ((line = keyboard.readLine()) != null) { output.println(line); if (line.equalsIgnoreCase("quit")) break; }
      reader.join(Duration.ofSeconds(1));
    }
  }
}
