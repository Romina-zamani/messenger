package dev.mahdi.chat.lesson1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** A deliberately small, single-client TCP server for the first lesson. */
public final class EchoServer {
    private final ServerSocket serverSocket;

    public EchoServer(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException error) {
            throw new IllegalStateException("Could not open server port " + port, error);
        }
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void runOnce() {
        System.out.println("Server listening on port " + port());
        System.out.println("Waiting for a client... accept() is blocking here.");

        try (serverSocket;
             Socket client = serverSocket.accept();
             BufferedReader input = new BufferedReader(new InputStreamReader(
                     client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8)) {

            System.out.println("Client connected from " + client.getRemoteSocketAddress());
            String message;
            while ((message = input.readLine()) != null) {
                System.out.println("Client says: " + message);
                if ("quit".equalsIgnoreCase(message.trim())) {
                    output.println("Goodbye!");
                    break;
                }
                output.println("Echo: " + message);
            }
            System.out.println("Connection closed.");
        } catch (IOException error) {
            throw new IllegalStateException("The server connection failed", error);
        }
    }

    public static void main(String[] args) {
        int port = args.length == 0 ? 5000 : Integer.parseInt(args[0]);
        new EchoServer(port).runOnce();
    }
}

