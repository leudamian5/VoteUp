package voteup.model;

import java.time.LocalDateTime;

/**
 * Directed follow relationship from {@code follower} to {@code followee}.
 */
public class Follow {

    private User follower;
    private User followee;
    private LocalDateTime createdAt;

    /**
     * @param follower user who follows
     * @param followee user being followed
     */
    public Follow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
        this.createdAt = LocalDateTime.now();
    }

    /** @return follower */
    public User getFollower() {
        return follower;
    }

    /** @return followee */
    public User getFollowee() {
        return followee;
    }

    /** @return when the follow was created */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
