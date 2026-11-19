package voteup.model.exception;

/**
 * Base checked exception for VoteUp domain and API failures.
 */
public abstract class VoteUpException extends Exception {

    /**
     * Creates an exception with a detail message.
     *
     * @param message detail message
     */
    public VoteUpException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a detail message and cause.
     *
     * @param message detail message
     * @param cause   underlying cause
     */
    public VoteUpException(String message, Throwable cause) {
        super(message, cause);
    }
}
