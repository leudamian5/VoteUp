package voteup.model.exception;

/**
 * The author of the parent post has blocked this user.
 */
public class ReplyBlockedException extends VoteUpException {

    /**
     * @param message detail message
     */
    public ReplyBlockedException(String message) {
        super(message);
    }
}
