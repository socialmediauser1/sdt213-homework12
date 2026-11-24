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

public class PlayersHandler implements HttpHandler {

    public static final String BASE_PATH = "/api/players";

    private final PlayerRepository repository;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public PlayersHandler(PlayerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
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
            sendError(exchange, 400, "The request body is not valid player JSON");
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

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

    private void getPlayer(HttpExchange exchange, int id) throws IOException {
        Optional<Player> player = repository.findById(id);
        if (player.isPresent()) {
            sendJson(exchange, 200, player.get());
        } else {
            sendError(exchange, 404, "Player with id " + id + " was not found");
        }
    }

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
        exchange.getResponseHeaders().set("Location", BASE_PATH + "/" + saved.id());
        sendJson(exchange, 201, saved);
    }

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
            sendError(exchange, 404, "Player with id " + id + " was not found");
        }
    }

    private void deletePlayer(HttpExchange exchange, int id) throws IOException {
        if (repository.delete(id)) {
            sendNoContent(exchange);
        } else {
            sendError(exchange, 404, "Player with id " + id + " was not found");
        }
    }

    private Player readPlayer(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Player player = gson.fromJson(body, Player.class);
        if (player == null) {
            throw new JsonParseException("empty body");
        }
        return player;
    }

    private String validate(Player player) {
        if (player.name() == null || player.name().isBlank()) {
            return "name is required";
        }
        if (player.position() == null) {
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
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        sendError(exchange, 405, "Method " + exchange.getRequestMethod() + " is not allowed here");
    }

    private void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        log(exchange, 204);
    }

    private void log(HttpExchange exchange, int status) {
        System.out.printf("%-6s %s -> %d%n", exchange.getRequestMethod(), exchange.getRequestURI(), status);
    }
}
