package voteup.model.exception;

import java.time.LocalDateTime;

/**
 * Login is temporarily locked due to progressive backoff after failed attempts.
 */
public class LockoutException extends VoteUpException {

    private final LocalDateTime unlockAt;

    /**
     * @param message  detail message
     * @param unlockAt when the lockout expires
     */
    public LockoutException(String message, LocalDateTime unlockAt) {
        super(message);
        this.unlockAt = unlockAt;
    }

    /**
     * @return unlock time
     */
    public LocalDateTime getUnlockAt() {
        return unlockAt;
    }
}
