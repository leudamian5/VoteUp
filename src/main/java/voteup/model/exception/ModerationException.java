package voteup.model.exception;

/**
 * A moderation action was rejected.
 */
public class ModerationException extends VoteUpException {

    /**
     * @param message detail message
     */
    public ModerationException(String message) {
        super(message);
    }
}
