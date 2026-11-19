package voteup.model.exception;

/**
 * Authentication failed or session is missing/invalid.
 */
public class AuthenticationException extends VoteUpException {

    /**
     * @param message detail message
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
