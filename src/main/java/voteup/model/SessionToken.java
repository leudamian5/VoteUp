package voteup.model;

import java.time.LocalDateTime;

/**
 * Opaque session issued after a successful login.
 */
public class SessionToken {

    private String tokenId;
    private User user;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private boolean revoked;

    /**
     * @param tokenId   unique token identifier
     * @param user      authenticated user
     * @param expiresAt expiration timestamp
     */
    public SessionToken(String tokenId, User user, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.user = user;
        this.issuedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    /** @return token id */
    public String getTokenId() {
        return tokenId;
    }

    /** @return owning user */
    public User getUser() {
        return user;
    }

    /** @return issue time */
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    /** @return expiry time */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /** @return whether the token was revoked */
    public boolean isRevoked() {
        return revoked;
    }

    /**
     * Whether this token is currently acceptable for API calls.
     *
     * @return {@code true} if not revoked and not expired
     */
    public boolean isValid() {
        return isValid(LocalDateTime.now());
    }

    /**
     * @param now current time
     * @return {@code true} if not revoked and {@code now} is before expiry
     */
    public boolean isValid(LocalDateTime now) {
        return !revoked && now.isBefore(expiresAt);
    }

    /**
     * Marks this token as revoked (logout, ban, or account delete).
     */
    public void revoke() {
        this.revoked = true;
    }
}
