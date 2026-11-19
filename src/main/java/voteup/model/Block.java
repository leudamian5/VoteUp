package voteup.model;

import java.time.LocalDateTime;

/**
 * Directed block relationship from {@code blocker} to {@code blocked}.
 * Creating a block also removes follows in both directions (design lock).
 */
public class Block {

    private User blocker;
    private User blocked;
    private LocalDateTime createdAt;

    /**
     * @param blocker user who blocks
     * @param blocked user being blocked
     */
    public Block(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = LocalDateTime.now();
    }

    /** @return blocker */
    public User getBlocker() {
        return blocker;
    }

    /** @return blocked user */
    public User getBlocked() {
        return blocked;
    }

    /** @return when the block was created */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
