package voteup.model.exception;

/**
 * A follow/block social-graph rule was violated.
 */
public class SocialException extends VoteUpException {

    /**
     * @param message detail message
     */
    public SocialException(String message) {
        super(message);
    }
}
