package ua.auk.hw12.model;

/**
 * The JSON body the server sends back when something goes wrong,
 * e.g. {"status":404,"error":"Player with id 42 was not found"}.
 */
public record ErrorMessage(int status, String error) {
}
