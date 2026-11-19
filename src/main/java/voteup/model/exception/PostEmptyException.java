package voteup.model.exception;

/**
 * A post or reply must contain text and/or an image.
 */
public class PostEmptyException extends VoteUpException {

    /**
     * @param message detail message
     */
    public PostEmptyException(String message) {
        super(message);
    }
}
