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

public class SquadServiceClient {

    private final String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Gson gson = new Gson();

    public SquadServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<Player> getAllPlayers() {
        HttpRequest request = newRequest("").GET().build();
        return List.of(gson.fromJson(send(request), Player[].class));
    }

    public List<Player> getPlayersByPosition(Position position) {
        HttpRequest request = newRequest("?position=" + position.name()).GET().build();
        return List.of(gson.fromJson(send(request), Player[].class));
    }

    public Player getPlayer(int id) {
        HttpRequest request = newRequest("/" + id).GET().build();
        return gson.fromJson(send(request), Player.class);
    }

    public Player addPlayer(Player player) {
        HttpRequest request = newRequest("")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(player), StandardCharsets.UTF_8))
                .build();
        return gson.fromJson(send(request), Player.class);
    }

    public Player updatePlayer(int id, Player player) {
        HttpRequest request = newRequest("/" + id)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(player), StandardCharsets.UTF_8))
                .build();
        return gson.fromJson(send(request), Player.class);
    }

    public void deletePlayer(int id) {
        HttpRequest request = newRequest("/" + id).DELETE().build();
        send(request);
    }

    private HttpRequest.Builder newRequest(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10));
    }

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

    private String readErrorMessage(String body) {
        try {
            ErrorMessage error = gson.fromJson(body, ErrorMessage.class);
            if (error != null && error.error() != null) {
                return error.error();
            }
        } catch (JsonSyntaxException e) {
        }
        return body;
    }
}
