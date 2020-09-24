package no.ntnu.datakomm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A Simple TCP server, used as a warm-up exercise for assignment A4.
 */
public class SimpleTcpServer {
    public static void main(String[] args) {
        SimpleTcpServer server = new SimpleTcpServer();
        log("Simple TCP server starting");
        server.run();
        log("ERROR: the server should never go out of the run() method! After handling one client");
    }

    public void run() {
        boolean mustRun = true;

        try {
            ServerSocket welcomeSocket = new ServerSocket(1301);
            while (mustRun) {
                Socket clientSocket = welcomeSocket.accept();
                System.out.println("Connection established");

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
            welcomeSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO - implement the logic of the server, according to the protocol.
        // Take a look at the tutorial to understand the basic blocks: creating a listening socket,
        // accepting the next client connection, sending and receiving messages and closing the connection
    }

    /**
     * Log a message to the system console.
     *
     * @param message The message to be logged (printed).
     */
    private static void log(String message) {
        System.out.println(message);
    }
}
