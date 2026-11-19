package voteup.model.exception;

/**
 * The requested user could not be found.
 */
public class UserNotFoundException extends VoteUpException {

    /**
     * @param message detail message
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
