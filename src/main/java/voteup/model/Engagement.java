package voteup.model;

import java.time.LocalDateTime;

/**
 * A user's like or dislike on a post. At most one engagement per (user, post).
 * Switching LIKE↔DISLIKE resets {@code createdAt} to now.
 */
public class Engagement {

    private String engagementId;
    private User user;
    private Post post;
    private EngagementType type;
    private LocalDateTime createdAt;

    /**
     * @param engagementId unique id
     * @param user         engaging user
     * @param post         target post
     * @param type         LIKE or DISLIKE
     */
    public Engagement(String engagementId, User user, Post post, EngagementType type) {
        this.engagementId = engagementId;
        this.user = user;
        this.post = post;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    /** @return engagement id */
    public String getEngagementId() {
        return engagementId;
    }

    /** @return user */
    public User getUser() {
        return user;
    }

    /** @return post */
    public Post getPost() {
        return post;
    }

    /** @return engagement type */
    public EngagementType getType() {
        return type;
    }

    /**
     * @param type new type (LIKE/DISLIKE switch)
     */
    public void setType(EngagementType type) {
        this.type = type;
    }

    /** @return creation / last-switch time */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt timestamp (reset on type switch)
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
