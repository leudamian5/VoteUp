package voteup.model.exception;

import java.time.LocalDateTime;

/**
 * Account cannot authenticate because of a temporary or permanent ban.
 */
public class BannedException extends VoteUpException {

    private final boolean permanent;
    private final LocalDateTime banExpiresAt;

    /**
     * @param message       detail message
     * @param permanent     {@code true} if permanently banned
     * @param banExpiresAt  expiry for temp ban; {@code null} if permanent
     */
    public BannedException(String message, boolean permanent, LocalDateTime banExpiresAt) {
        super(message);
        this.permanent = permanent;
        this.banExpiresAt = banExpiresAt;
    }

    /**
     * @return whether the ban is permanent
     */
    public boolean isPermanent() {
        return permanent;
    }

    /**
     * @return temp-ban expiry, or {@code null} if permanent
     */
    public LocalDateTime getBanExpiresAt() {
        return banExpiresAt;
    }
}
