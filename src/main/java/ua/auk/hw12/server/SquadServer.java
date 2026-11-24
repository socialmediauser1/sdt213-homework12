package ua.auk.hw12.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class SquadServer {

    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext(PlayersHandler.BASE_PATH, new PlayersHandler(new PlayerRepository()));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Squad Manager REST service is running at http://localhost:" + PORT + PlayersHandler.BASE_PATH);
        System.out.println("Press Ctrl+C to stop it.");
    }
}
