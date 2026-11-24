package ua.auk.hw12.client;

import ua.auk.hw12.model.Player;
import ua.auk.hw12.model.Position;

import java.util.List;
import java.util.Scanner;

/**
 * Console program that demonstrates the Squad Manager web service.
 * Every menu item calls one REST endpoint through {@link SquadServiceClient}.
 * Start SquadServer first, then run this program.
 */
public class ClientApp {

    // Address of the players resource of the web service (SquadServer must be running)
    private static final String SERVICE_URL = "http://localhost:8080/api/players";

    private final Scanner scanner = new Scanner(System.in);
    private final SquadServiceClient client;

    public ClientApp(SquadServiceClient client) {
        this.client = client;
    }

    public static void main(String[] args) {
        new ClientApp(new SquadServiceClient(SERVICE_URL)).run();
    }

    private void run() {
        System.out.println("=== Squad Manager - client of my REST web service ===");
        System.out.println("Service: " + SERVICE_URL);
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            if (choice.equals("0")) {
                System.out.println("Goodbye!");
                return;
            }
            try {
                switch (choice) {
                    case "1" -> showAllPlayers();
                    case "2" -> showPlayersByPosition();
                    case "3" -> showOnePlayer();
                    case "4" -> addPlayer();
                    case "5" -> updateGoals();
                    case "6" -> deletePlayer();
                    default -> System.out.println("Please enter a number from 0 to 6.");
                }
            } catch (SquadServiceException e) {
                // error reported by the web service (404, 400, 409 ...) or server not running
                System.out.println("Error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                // wrong input typed by the user (e.g. letters instead of a number)
                System.out.println("Invalid input: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. Show all players            (GET    /api/players)");
        System.out.println("2. Show players by position    (GET    /api/players?position=...)");
        System.out.println("3. Find a player by id         (GET    /api/players/{id})");
        System.out.println("4. Add a new player            (POST   /api/players)");
        System.out.println("5. Change a player's goals     (PUT    /api/players/{id})");
        System.out.println("6. Delete a player             (DELETE /api/players/{id})");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    // ----- menu actions -----

    private void showAllPlayers() {
        printTable(client.getAllPlayers());
    }

    private void showPlayersByPosition() {
        Position position = readPosition();
        printTable(client.getPlayersByPosition(position));
    }

    private void showOnePlayer() {
        int id = readInt("Player id: ");
        printTable(List.of(client.getPlayer(id)));
    }

    private void addPlayer() {
        String name = readText("Name: ");
        Position position = readPosition();
        int shirtNumber = readInt("Shirt number (1-99): ");
        String nationality = readText("Nationality: ");
        int goals = readInt("Goals: ");

        // id 0 = "not known yet"; the server chooses the real id
        Player saved = client.addPlayer(new Player(0, name, position, shirtNumber, nationality, goals));
        System.out.println("Added. The server gave the new player id " + saved.id() + ":");
        printTable(List.of(saved));
    }

    private void updateGoals() {
        int id = readInt("Player id: ");
        // 1) GET the current version, 2) change one field, 3) PUT the whole player back
        Player player = client.getPlayer(id);
        System.out.println(player.name() + " has " + player.goals() + " goals now.");
        int goals = readInt("New number of goals: ");
        Player saved = client.updatePlayer(id, player.withGoals(goals));
        System.out.println("Updated:");
        printTable(List.of(saved));
    }

    private void deletePlayer() {
        int id = readInt("Player id: ");
        client.deletePlayer(id);
        System.out.println("Player " + id + " was deleted.");
    }

    // ----- input and output helpers -----

    private String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        String text = readText(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a whole number");
        }
    }

    private Position readPosition() {
        String text = readText("Position (GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD): ");
        try {
            return Position.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + text + "' is not a position");
        }
    }

    private void printTable(List<Player> players) {
        if (players.isEmpty()) {
            System.out.println("No players found.");
            return;
        }
        String format = "%-4s %-20s %-11s %-4s %-12s %s%n";
        System.out.printf(format, "ID", "Name", "Position", "No.", "Nationality", "Goals");
        System.out.println("-".repeat(60));
        for (Player p : players) {
            System.out.printf(format, p.id(), p.name(), p.position(), p.shirtNumber(), p.nationality(), p.goals());
        }
    }
}
