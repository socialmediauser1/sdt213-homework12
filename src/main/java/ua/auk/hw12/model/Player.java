package ua.auk.hw12.model;

/**
 * One player of the squad. This is the resource that the REST service manages.
 * <p>
 * Gson turns it into JSON like:
 * {"id":1,"name":"Oleh Koval","position":"GOALKEEPER","shirtNumber":1,"nationality":"Ukraine","goals":0}
 * <p>
 * The id is chosen by the server. When a client creates a player it leaves the id out (it becomes 0).
 */
public record Player(int id,
                     String name,
                     Position position,
                     int shirtNumber,
                     String nationality,
                     int goals) {

    /** Returns a copy of this player with another id (records are immutable). */
    public Player withId(int newId) {
        return new Player(newId, name, position, shirtNumber, nationality, goals);
    }

    /** Returns a copy of this player with another number of goals. */
    public Player withGoals(int newGoals) {
        return new Player(id, name, position, shirtNumber, nationality, newGoals);
    }
}
