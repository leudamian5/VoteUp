package voteup.model.exception;

/**
 * The actor is not allowed to perform this operation.
 */
public class AuthorizationException extends VoteUpException {

    /**
     * @param message detail message
     */
    public AuthorizationException(String message) {
        super(message);
    }
}
