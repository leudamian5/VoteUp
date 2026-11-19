package voteup.model.exception;

/**
 * The profile cannot be viewed (e.g. due to a block).
 */
public class ProfileUnavailableException extends VoteUpException {

    /**
     * @param message detail message
     */
    public ProfileUnavailableException(String message) {
        super(message);
    }
}
