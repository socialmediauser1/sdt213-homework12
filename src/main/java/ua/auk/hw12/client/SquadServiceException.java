package ua.auk.hw12.client;

/**
 * Thrown by {@link SquadServiceClient} when the web service cannot be reached
 * or answers with an error status code (4xx or 5xx).
 */
public class SquadServiceException extends RuntimeException {

    public SquadServiceException(String message) {
        super(message);
    }
}
