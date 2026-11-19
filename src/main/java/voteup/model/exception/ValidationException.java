package voteup.model.exception;

/**
 * Input validation failed.
 */
public class ValidationException extends VoteUpException {

    /**
     * @param message detail message
     */
    public ValidationException(String message) {
        super(message);
    }
}
