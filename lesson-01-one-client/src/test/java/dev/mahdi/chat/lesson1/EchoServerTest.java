package dev.mahdi.chat.lesson1;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class EchoServerTest {

    @Test
    void echoesAClientMessageAndClosesOnQuit() {
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            EchoServer server = new EchoServer(0);
            CompletableFuture<Void> running = CompletableFuture.runAsync(server::runOnce);

            try (Socket socket = new Socket("localhost", server.port());
                 BufferedReader input = new BufferedReader(new InputStreamReader(
                         socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                output.println("Hello server");
                assertEquals("Echo: Hello server", input.readLine());

                output.println("quit");
                assertEquals("Goodbye!", input.readLine());
            }

            running.join();
        });
    }
}
