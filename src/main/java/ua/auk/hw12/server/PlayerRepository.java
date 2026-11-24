package ua.auk.hw12.server;

import ua.auk.hw12.model.Player;
import ua.auk.hw12.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Stores the players in memory (no database), so the data is reset when the server restarts.
 * <p>
 * The HTTP server handles requests on several threads at once, so every method is
 * {@code synchronized}: only one thread can read or change the map at a time.
 */
public class PlayerRepository {

    // TreeMap keeps the players sorted by id
    private final Map<Integer, Player> players = new TreeMap<>();
    private int nextId = 1;

    /** Fills the repository with a few players so the service is not empty at start. */
    public PlayerRepository() {
        add(new Player(0, "Oleh Koval", Position.GOALKEEPER, 1, "Ukraine", 0));
        add(new Player(0, "Mark Novak", Position.DEFENDER, 4, "Poland", 1));
        add(new Player(0, "Taras Bondar", Position.DEFENDER, 5, "Ukraine", 2));
        add(new Player(0, "Lucas Ferreira", Position.MIDFIELDER, 8, "Brazil", 6));
        add(new Player(0, "Andrii Shevchuk", Position.MIDFIELDER, 10, "Ukraine", 9));
        add(new Player(0, "Ivan Hrytsenko", Position.FORWARD, 9, "Ukraine", 14));
        add(new Player(0, "Jonas Weber", Position.FORWARD, 11, "Germany", 7));
    }

    public synchronized List<Player> findAll() {
        return new ArrayList<>(players.values());
    }

    public synchronized List<Player> findByPosition(Position position) {
        List<Player> result = new ArrayList<>();
        for (Player player : players.values()) {
            if (player.position() == position) {
                result.add(player);
            }
        }
        return result;
    }

    public synchronized Optional<Player> findById(int id) {
        return Optional.ofNullable(players.get(id));
    }

    /** Saves a new player under the next free id and returns the saved player. */
    public synchronized Player add(Player player) {
        Player saved = player.withId(nextId);
        players.put(nextId, saved);
        nextId++;
        return saved;
    }

    /** Replaces the player with this id. Returns empty if there is no such player. */
    public synchronized Optional<Player> update(int id, Player player) {
        if (!players.containsKey(id)) {
            return Optional.empty();
        }
        Player saved = player.withId(id);
        players.put(id, saved);
        return Optional.of(saved);
    }

    /** Removes the player. Returns false if there was no player with this id. */
    public synchronized boolean delete(int id) {
        return players.remove(id) != null;
    }

    /**
     * Checks if another player (not the one with {@code ignoredId}) already wears this shirt number.
     * Pass 0 as ignoredId when adding a new player.
     */
    public synchronized boolean isShirtNumberTaken(int shirtNumber, int ignoredId) {
        for (Player player : players.values()) {
            if (player.shirtNumber() == shirtNumber && player.id() != ignoredId) {
                return true;
            }
        }
        return false;
    }
}
