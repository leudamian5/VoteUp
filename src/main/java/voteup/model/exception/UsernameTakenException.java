package voteup.model.exception;

/**
 * The requested username is already in use by an active account.
 */
public class UsernameTakenException extends VoteUpException {

    /**
     * @param message detail message
     */
    public UsernameTakenException(String message) {
        super(message);
    }
}
