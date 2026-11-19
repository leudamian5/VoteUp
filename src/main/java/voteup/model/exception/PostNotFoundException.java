package voteup.model.exception;

/**
 * The requested post was not found or is deleted.
 */
public class PostNotFoundException extends VoteUpException {

    /**
     * @param message detail message
     */
    public PostNotFoundException(String message) {
        super(message);
    }
}
