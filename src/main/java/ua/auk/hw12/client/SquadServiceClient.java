package ua.auk.hw12.client;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import ua.auk.hw12.model.ErrorMessage;
import ua.auk.hw12.model.Player;
import ua.auk.hw12.model.Position;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Talks to the Squad Manager REST service over HTTP.
 * Each public method sends one request and turns the JSON answer back into Java objects,
 * so the rest of the program does not need to know about HTTP or JSON.
 */
public class SquadServiceClient {

    private final String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Gson gson = new Gson();

    /** @param baseUrl address of the players resource, e.g. http://localhost:8080/api/players */
    public SquadServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** GET /api/players */
    public List<Player> getAllPlayers() {
        HttpRequest request = newRequest("").GET().build();
        return List.of(gson.fromJson(send(request), Player[].class));
    }

    /** GET /api/players?position=... */
    public List<Player> getPlayersByPosition(Position position) {
        HttpRequest request = newRequest("?position=" + position.name()).GET().build();
        return List.of(gson.fromJson(send(request), Player[].class));
    }

    /** GET /api/players/{id} */
    public Player getPlayer(int id) {
        HttpRequest request = newRequest("/" + id).GET().build();
        return gson.fromJson(send(request), Player.class);
    }

    /** POST /api/players - returns the saved player with the id the server gave it. */
    public Player addPlayer(Player player) {
        HttpRequest request = newRequest("")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(player), StandardCharsets.UTF_8))
                .build();
        return gson.fromJson(send(request), Player.class);
    }

    /** PUT /api/players/{id} - replaces the player and returns the saved version. */
    public Player updatePlayer(int id, Player player) {
        HttpRequest request = newRequest("/" + id)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(player), StandardCharsets.UTF_8))
                .build();
        return gson.fromJson(send(request), Player.class);
    }

    /** DELETE /api/players/{id} */
    public void deletePlayer(int id) {
        HttpRequest request = newRequest("/" + id).DELETE().build();
        send(request);
    }

    /** Starts building a request to baseUrl + path that expects JSON back. */
    private HttpRequest.Builder newRequest(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10));
    }

    /**
     * Sends the request and returns the response body.
     * Throws SquadServiceException if the server is down or the status code is not 2xx.
     */
    private String send(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (ConnectException e) {
            throw new SquadServiceException("Cannot connect to " + baseUrl + ". Is SquadServer running?");
        } catch (IOException e) {
            throw new SquadServiceException("Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SquadServiceException("The request was interrupted");
        }

        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        }
        throw new SquadServiceException("HTTP " + status + ": " + readErrorMessage(response.body()));
    }

    /** The server sends errors as {"status":...,"error":"..."}; take the text out of it. */
    private String readErrorMessage(String body) {
        try {
            ErrorMessage error = gson.fromJson(body, ErrorMessage.class);
            if (error != null && error.error() != null) {
                return error.error();
            }
        } catch (JsonSyntaxException e) {
            // the body was not our error JSON, show it as it is
        }
        return body;
    }
}
