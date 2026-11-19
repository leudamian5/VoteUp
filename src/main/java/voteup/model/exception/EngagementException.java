package voteup.model.exception;

/**
 * A like/dislike engagement could not be applied.
 */
public class EngagementException extends VoteUpException {

    /**
     * @param message detail message
     */
    public EngagementException(String message) {
        super(message);
    }
}
