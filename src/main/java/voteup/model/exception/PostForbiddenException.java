package voteup.model.exception;

/**
 * The actor may not modify or delete this post.
 */
public class PostForbiddenException extends VoteUpException {

    /**
     * @param message detail message
     */
    public PostForbiddenException(String message) {
        super(message);
    }
}
