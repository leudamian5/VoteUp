package voteup.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import voteup.model.exception.SocialException;

/**
 * Registered VoteUp account. Holds profile, role, ban/lockout state, and achievements.
 */
public class User {

    private String userId;
    private String username;
    private String passwordHash;
    private String bio;
    private String profilePictureUrl;
    private Role role;
    private boolean isDeleted;
    private boolean isPermanentlyBanned;
    private LocalDateTime banExpiresAt;
    private int failedLoginAttempts;
    private LocalDateTime lockoutExpiresAt;
    private LocalDateTime lastFailedLogin;
    /** 0 = next lock 15m; 1 = next lock 30m. */
    private int lockoutTier;
    private LocalDateTime lastLockEndedAt;
    private LocalDateTime createdAt;
    private List<Achievement> achievements;

    /**
     * Creates a new active user with role {@link Role#USER}.
     *
     * @param userId       unique id
     * @param username     unique among active users
     * @param passwordHash hashed password
     */
    public User(String userId, String username, String passwordHash) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = Role.USER;
        this.isDeleted = false;
        this.isPermanentlyBanned = false;
        this.failedLoginAttempts = 0;
        this.lockoutTier = 0;
        this.createdAt = LocalDateTime.now();
        this.achievements = new ArrayList<>();
    }

    /** @return user id */
    public String getUserId() {
        return userId;
    }

    /** @return username */
    public String getUsername() {
        return username;
    }

    /**
     * @param username new username (e.g. rename on delete)
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @return password hash */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @return bio text */
    public String getBio() {
        return bio;
    }

    /** @param bio profile bio */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /** @return profile picture URL or local path */
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    /** @param profilePictureUrl avatar location */
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    /** @return role */
    public Role getRole() {
        return role;
    }

    /** @param role new role */
    public void setRole(Role role) {
        this.role = role;
    }

    /** @return whether the account is soft-deleted */
    public boolean isDeleted() {
        return isDeleted;
    }

    /** @param deleted soft-delete flag */
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    /** @return whether permanently banned */
    public boolean isPermanentlyBanned() {
        return isPermanentlyBanned;
    }

    /** @param permanentlyBanned permanent ban flag */
    public void setPermanentlyBanned(boolean permanentlyBanned) {
        isPermanentlyBanned = permanentlyBanned;
    }

    /** @return temp ban expiry or {@code null} */
    public LocalDateTime getBanExpiresAt() {
        return banExpiresAt;
    }

    /** @param banExpiresAt temp ban expiry */
    public void setBanExpiresAt(LocalDateTime banExpiresAt) {
        this.banExpiresAt = banExpiresAt;
    }

    /** @return failed login attempt count */
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    /** @param failedLoginAttempts attempt count */
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    /** @return login lockout expiry */
    public LocalDateTime getLockoutExpiresAt() {
        return lockoutExpiresAt;
    }

    /** @param lockoutExpiresAt lockout expiry */
    public void setLockoutExpiresAt(LocalDateTime lockoutExpiresAt) {
        this.lockoutExpiresAt = lockoutExpiresAt;
    }

    /** @return last failed login time */
    public LocalDateTime getLastFailedLogin() {
        return lastFailedLogin;
    }

    /** @param lastFailedLogin last failure time */
    public void setLastFailedLogin(LocalDateTime lastFailedLogin) {
        this.lastFailedLogin = lastFailedLogin;
    }

    /** @return lockout progression tier */
    public int getLockoutTier() {
        return lockoutTier;
    }

    /** @param lockoutTier progression tier */
    public void setLockoutTier(int lockoutTier) {
        this.lockoutTier = lockoutTier;
    }

    /** @return when the last lockout ended */
    public LocalDateTime getLastLockEndedAt() {
        return lastLockEndedAt;
    }

    /** @param lastLockEndedAt last lock end time */
    public void setLastLockEndedAt(LocalDateTime lastLockEndedAt) {
        this.lastLockEndedAt = lastLockEndedAt;
    }

    /** @return account creation time */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets account creation time (model clock / feed tie-break tests).
     *
     * @param createdAt creation timestamp
     */
    /**
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** @return achievements list */
    public List<Achievement> getAchievements() {
        return achievements;
    }

    /**
     * Follows another user.
     *
     * @param target user to follow
     * @throws SocialException if the follow is not allowed
     */
    public void follow(User target) throws SocialException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Unfollows another user.
     *
     * @param target user to unfollow
     * @throws SocialException if the unfollow is not allowed
     */
    public void unfollow(User target) throws SocialException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Blocks another user and removes follows in both directions.
     *
     * @param target user to block
     * @throws SocialException if the block is not allowed
     */
    public void block(User target) throws SocialException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Removes a block on another user (does not restore follows).
     *
     * @param target user to unblock
     * @throws SocialException if the unblock is not allowed
     */
    public void unblock(User target) throws SocialException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * @return {@code true} if a temporary ban is currently active
     */
    public boolean isTempBanned() {
        return isTempBanned(LocalDateTime.now());
    }

    /**
     * @param now current time
     * @return {@code true} if a temporary ban is active at {@code now}
     */
    public boolean isTempBanned(LocalDateTime now) {
        return banExpiresAt != null && now.isBefore(banExpiresAt);
    }

    /**
     * @return {@code true} if a login lockout is currently active
     */
    public boolean isLockedOut() {
        return isLockedOut(LocalDateTime.now());
    }

    /**
     * @param now current time
     * @return {@code true} if login lockout is active at {@code now}
     */
    public boolean isLockedOut(LocalDateTime now) {
        return lockoutExpiresAt != null && now.isBefore(lockoutExpiresAt);
    }
}
