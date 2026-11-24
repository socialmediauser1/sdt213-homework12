package ua.auk.hw12.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Starts the Squad Manager REST web service.
 * <p>
 * It uses {@link HttpServer}, the small HTTP server that is built into the JDK,
 * so no web framework or application server is needed.
 * The service runs until the program is stopped (Ctrl+C or the Stop button in IDEA).
 */
public class SquadServer {

    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        // Listen on port 8080 of this computer (0 = default queue length for waiting connections)
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Every request whose path starts with /api/players goes to PlayersHandler
        server.createContext(PlayersHandler.BASE_PATH, new PlayersHandler(new PlayerRepository()));

        // Handle up to 4 requests at the same time, each on its own thread
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Squad Manager REST service is running at http://localhost:" + PORT + PlayersHandler.BASE_PATH);
        System.out.println("Press Ctrl+C to stop it.");
    }
}
