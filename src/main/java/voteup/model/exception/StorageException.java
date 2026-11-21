package voteup.model.exception;

/**
 * Local persistence (e.g. H2) failed.
 */
public class StorageException extends VoteUpException {

    /**
     * @param message detail message
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * @param message detail message
     * @param cause   underlying cause
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
