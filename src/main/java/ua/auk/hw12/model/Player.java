package ua.auk.hw12.model;

public record Player(int id,
                     String name,
                     Position position,
                     int shirtNumber,
                     String nationality,
                     int goals) {

    public Player withId(int newId) {
        return new Player(newId, name, position, shirtNumber, nationality, goals);
    }

    public Player withGoals(int newGoals) {
        return new Player(id, name, position, shirtNumber, nationality, newGoals);
    }
}
