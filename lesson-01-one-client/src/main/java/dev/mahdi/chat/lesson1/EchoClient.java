package dev.mahdi.chat.lesson1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Console client: type a line, wait for one server response, repeat. */
public final class EchoClient {
    private EchoClient() {
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        try (Socket socket = new Socket(host, port);
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            System.out.println("Connected to " + host + ":" + port);
            System.out.println("Type a message, or type quit to close:");
            String message;
            while ((message = keyboard.readLine()) != null) {
                serverOutput.println(message);
                String response = serverInput.readLine();
                if (response == null) {
                    System.out.println("Server closed the connection.");
                    break;
                }
                System.out.println("Server says: " + response);
                if ("quit".equalsIgnoreCase(message.trim())) {
                    break;
                }
            }
        } catch (ConnectException error) {
            System.err.println("Could not connect. Start EchoServer first and check port " + port + ".");
        } catch (IOException error) {
            System.err.println("Network error: " + error.getMessage());
        }
    }
}

