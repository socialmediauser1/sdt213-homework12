# SDT213 Homework 12 – Creating Web Services

**Squad Manager** – my own **REST web service** that manages the players of a football squad,
plus a **JavaFX client** (and a console client) that uses it.

In Homework 11 the program was only a *client* of someone else's service (football-data.org).
Here both sides are mine: `SquadServer` is the web service, `SquadApp` (JavaFX) and `ConsoleApp` are programs that call it.

```
 SquadApp  ──► SquadServiceClient ──HTTP + JSON──► SquadServer ──► PlayersHandler ──► PlayerRepository
 (JavaFX)      (java.net.http.HttpClient)          (JDK HttpServer)  (routes requests)    (players in memory)
```

## REST API

Base address: `http://localhost:8080/api/players`. Request and response bodies are JSON.

| Method | Path | What it does | Success | Errors |
|---|---|---|---|---|
| GET | `/api/players` | list all players | 200 | |
| GET | `/api/players?position=FORWARD` | list players of one position | 200 | 400 unknown position |
| GET | `/api/players/{id}` | one player | 200 | 404 |
| POST | `/api/players` | add a player (server chooses the id) | 201 + `Location` header | 400 invalid data, 409 shirt number taken |
| PUT | `/api/players/{id}` | replace a player | 200 | 400, 404, 409 |
| DELETE | `/api/players/{id}` | remove a player | 204 (no body) | 404 |

Any other method returns **405 Method Not Allowed** with an `Allow` header.

Player JSON:

```json
{"id":6,"name":"Ivan Hrytsenko","position":"FORWARD","shirtNumber":9,"nationality":"Ukraine","goals":14}
```

`position` is one of `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD`; `shirtNumber` is 1–99 and unique; `goals` ≥ 0.

Errors look like:

```json
{"status":404,"error":"Player with id 42 was not found"}
```

## Classes

| Class | Role |
|---|---|
| `model.Player` | The resource (a Java record). Gson converts it to/from JSON |
| `model.Position` | Enum of the four positions |
| `model.ErrorMessage` | JSON body of error responses |
| `server.SquadServer` | `main` – creates the JDK `HttpServer` on port 8080 and registers the handler |
| `server.PlayersHandler` | Reads the HTTP method and path, calls the right endpoint method, validates input, writes JSON + status code |
| `server.PlayerRepository` | Stores players in a `TreeMap` (in memory, thread-safe with `synchronized`), starts with 7 players |
| `client.SquadServiceClient` | One method per endpoint; builds `HttpRequest`s, sends them, checks the status code, parses JSON |
| `client.SquadServiceException` | Error from the service (or the service is not running) |
| `gui.SquadApp` | JavaFX window: table of players with position filter and find by id, status bar showing each request and its result; requests run in a background `Task` |
| `gui.PlayerForm` | The form on the right: fields of a player and the Add (POST), Save (PUT), Delete (DELETE) and Clear buttons |
| `gui.Launcher` | `main` – starts the JavaFX application |
| `client.ConsoleApp` | `main` – console menu with the same features |

## How to run

Requires Java 17+. Run the server first, then the client in a second terminal (or a second Run configuration in IDEA).

1. Server: `.\mvnw.cmd -q compile exec:java`
   (or in IntelliJ IDEA run `ua.auk.hw12.server.SquadServer`)
2. Client window: `.\mvnw.cmd -q javafx:run`
   (or run `ua.auk.hw12.gui.Launcher`)
3. Console client (optional): `.\mvnw.cmd -q exec:java "-Dexec.mainClass=ua.auk.hw12.client.ConsoleApp"`

The service can also be tested without the client, e.g. open `http://localhost:8080/api/players` in a browser, or:

```
curl http://localhost:8080/api/players/3
curl -X POST -H "Content-Type: application/json" -d "{\"name\":\"Petro Melnyk\",\"position\":\"MIDFIELDER\",\"shirtNumber\":17,\"nationality\":\"Ukraine\",\"goals\":3}" http://localhost:8080/api/players
curl -X DELETE http://localhost:8080/api/players/8
```

The server prints every request it answers, e.g. `POST   /api/players -> 201`.
Data is kept in memory, so it resets every time the server restarts.
