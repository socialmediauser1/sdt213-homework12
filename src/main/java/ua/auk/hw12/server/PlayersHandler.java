package ua.auk.hw12.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.auk.hw12.model.ErrorMessage;
import ua.auk.hw12.model.Player;
import ua.auk.hw12.model.Position;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Handles every HTTP request that starts with /api/players.
 * <p>
 * REST endpoints (the resource is "players", the HTTP method says what to do with it):
 * <pre>
 *   GET    /api/players                    list all players     -> 200
 *   GET    /api/players?position=FORWARD   list by position     -> 200
 *   GET    /api/players/{id}               one player           -> 200 or 404
 *   POST   /api/players                    add a player         -> 201 or 400 / 409
 *   PUT    /api/players/{id}               replace a player     -> 200 or 400 / 404 / 409
 *   DELETE /api/players/{id}               remove a player      -> 204 or 404
 * </pre>
 * Request and response bodies are JSON.
 */
public class PlayersHandler implements HttpHandler {

    public static final String BASE_PATH = "/api/players";

    private final PlayerRepository repository;
    // disableHtmlEscaping: write characters like ' and < as they are instead of escape codes
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public PlayersHandler(PlayerRepository repository) {
        this.repository = repository;
    }

    /** Called by the HttpServer for each request. Decides which method below should answer it. */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        // The part of the path after "/api/players": "" or "/" for the collection, "/5" for one player
        String rest = exchange.getRequestURI().getPath().substring(BASE_PATH.length());

        try {
            if (rest.isEmpty() || rest.equals("/")) {
                switch (method) {
                    case "GET" -> listPlayers(exchange);
                    case "POST" -> createPlayer(exchange);
                    default -> sendMethodNotAllowed(exchange, "GET, POST");
                }
            } else {
                Integer id = parseId(rest);
                if (id == null) {
                    sendError(exchange, 404, "Unknown address " + exchange.getRequestURI().getPath());
                    return;
                }
                switch (method) {
                    case "GET" -> getPlayer(exchange, id);
                    case "PUT" -> updatePlayer(exchange, id);
                    case "DELETE" -> deletePlayer(exchange, id);
                    default -> sendMethodNotAllowed(exchange, "GET, PUT, DELETE");
                }
            }
        } catch (JsonParseException e) {
            // Gson could not read the body, e.g. a missing bracket or text where a number is expected
            sendError(exchange, 400, "The request body is not valid player JSON");
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    // ----- endpoints -----

    /** GET /api/players and GET /api/players?position=... */
    private void listPlayers(HttpExchange exchange) throws IOException {
        String positionText = getQueryParameter(exchange, "position");
        if (positionText == null) {
            sendJson(exchange, 200, repository.findAll());
            return;
        }
        Position position = parsePosition(positionText);
        if (position == null) {
            sendError(exchange, 400, "Unknown position '" + positionText + "'. Use GOALKEEPER, DEFENDER, MIDFIELDER or FORWARD");
            return;
        }
        sendJson(exchange, 200, repository.findByPosition(position));
    }

    /** GET /api/players/{id} */
    private void getPlayer(HttpExchange exchange, int id) throws IOException {
        Optional<Player> player = repository.findById(id);
        if (player.isPresent()) {
            sendJson(exchange, 200, player.get());
        } else {
            sendError(exchange, 404, "Player with id " + id + " was not found");
        }
    }

    /** POST /api/players  - body: a player without id */
    private void createPlayer(HttpExchange exchange) throws IOException {
        Player player = readPlayer(exchange);
        String problem = validate(player);
        if (problem != null) {
            sendError(exchange, 400, problem);
            return;
        }
        if (repository.isShirtNumberTaken(player.shirtNumber(), 0)) {
            sendError(exchange, 409, "Shirt number " + player.shirtNumber() + " is already taken");
            return;
        }
        Player saved = repository.add(player);
        // 201 Created + Location header with the address of the new resource
        exchange.getResponseHeaders().set("Location", BASE_PATH + "/" + saved.id());
        sendJson(exchange, 201, saved);
    }

    /** PUT /api/players/{id}  - body: the full new version of the player */
    private void updatePlayer(HttpExchange exchange, int id) throws IOException {
        if (repository.findById(id).isEmpty()) {
            sendError(exchange, 404, "Player with id " + id + " was not found");
            return;
        }
        Player player = readPlayer(exchange);
        String problem = validate(player);
        if (problem != null) {
            sendError(exchange, 400, problem);
            return;
        }
        if (repository.isShirtNumberTaken(player.shirtNumber(), id)) {
            sendError(exchange, 409, "Shirt number " + player.shirtNumber() + " is already taken");
            return;
        }
        Optional<Player> saved = repository.update(id, player);
        if (saved.isPresent()) {
            sendJson(exchange, 200, saved.get());
        } else {
            // the player was deleted by another request in the meantime
            sendError(exchange, 404, "Player with id " + id + " was not found");
        }
    }

    /** DELETE /api/players/{id} */
    private void deletePlayer(HttpExchange exchange, int id) throws IOException {
        if (repository.delete(id)) {
            sendNoContent(exchange);
        } else {
            sendError(exchange, 404, "Player with id " + id + " was not found");
        }
    }

    // ----- reading the request -----

    /** Reads the request body and converts the JSON into a Player. */
    private Player readPlayer(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Player player = gson.fromJson(body, Player.class);
        if (player == null) {
            // empty body
            throw new JsonParseException("empty body");
        }
        return player;
    }

    /** Returns a message describing what is wrong with the player, or null if it is valid. */
    private String validate(Player player) {
        if (player.name() == null || player.name().isBlank()) {
            return "name is required";
        }
        if (player.position() == null) {
            // Gson leaves the field null when it is missing or not one of the enum names
            return "position is required: GOALKEEPER, DEFENDER, MIDFIELDER or FORWARD";
        }
        if (player.shirtNumber() < 1 || player.shirtNumber() > 99) {
            return "shirtNumber must be between 1 and 99";
        }
        if (player.nationality() == null || player.nationality().isBlank()) {
            return "nationality is required";
        }
        if (player.goals() < 0) {
            return "goals cannot be negative";
        }
        return null;
    }

    /** Turns "/5" into 5. Returns null for anything that is not "/" followed by a number. */
    private Integer parseId(String rest) {
        try {
            return Integer.parseInt(rest.substring(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Position parsePosition(String text) {
        try {
            return Position.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Finds a value in the query string, e.g. "position" in "?position=FORWARD". */
    private String getQueryParameter(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] keyAndValue = pair.split("=", 2);
            if (keyAndValue[0].equals(name) && keyAndValue.length == 2) {
                return keyAndValue[1];
            }
        }
        return null;
    }

    // ----- writing the response -----

    /** Converts the object to JSON and sends it with the given status code. */
    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
        log(exchange, status);
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, new ErrorMessage(status, message));
    }

    private void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        // the Allow header tells the client which methods it may use on this address
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        sendError(exchange, 405, "Method " + exchange.getRequestMethod() + " is not allowed here");
    }

    /** 204 No Content: success, but there is no body (used after DELETE). */
    private void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1); // -1 means "no body"
        log(exchange, 204);
    }

    /** Prints every request to the server console, e.g. "GET /api/players/3 -> 200". */
    private void log(HttpExchange exchange, int status) {
        System.out.printf("%-6s %s -> %d%n", exchange.getRequestMethod(), exchange.getRequestURI(), status);
    }
}
