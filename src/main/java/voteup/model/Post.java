package voteup.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User-authored post or reply. Text and image are immutable after construction;
 * tags may change. Soft-deleted posts remain stored with {@code isDeleted=true}.
 */
public class Post {

    private String postId;
    private User author;
    private String textContent;
    private String imageUrl;
    private LocalDateTime createdAt;
    private Post parentPost;
    private List<Tag> tags;
    private boolean isDeleted;
    private User deletedBy;
    private int likeCount;
    private int dislikeCount;

    /**
     * Creates a top-level post.
     *
     * @param postId      unique id
     * @param author      author
     * @param textContent text (may be null/blank if image present)
     * @param imageUrl    image path/URL (may be null if text present)
     */
    public Post(String postId, User author, String textContent, String imageUrl) {
        this(postId, author, textContent, imageUrl, null);
    }

    /**
     * Creates a post or reply.
     *
     * @param postId      unique id
     * @param author      author
     * @param textContent text
     * @param imageUrl    image
     * @param parent      parent post for replies; {@code null} for top-level
     */
    public Post(String postId, User author, String textContent, String imageUrl, Post parent) {
        this.postId = postId;
        this.author = author;
        this.textContent = textContent;
        this.imageUrl = imageUrl;
        this.parentPost = parent;
        this.createdAt = LocalDateTime.now();
        this.tags = new ArrayList<>();
        this.isDeleted = false;
        this.likeCount = 0;
        this.dislikeCount = 0;
    }

    /** @return post id */
    public String getPostId() {
        return postId;
    }

    /** @return author */
    public User getAuthor() {
        return author;
    }

    /** @return text content */
    public String getTextContent() {
        return textContent;
    }

    /** @return image URL/path */
    public String getImageUrl() {
        return imageUrl;
    }

    /** @return creation time */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets creation time (model clock / tests).
     *
     * @param createdAt creation timestamp
     */
    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** @return parent post or {@code null} */
    public Post getParentPost() {
        return parentPost;
    }

    /** @return mutable tag list */
    public List<Tag> getTags() {
        return tags;
    }

    /** @return soft-delete flag */
    public boolean isDeleted() {
        return isDeleted;
    }

    /** @param deleted soft-delete flag */
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    /** @return who deleted the post */
    public User getDeletedBy() {
        return deletedBy;
    }

    /** @param deletedBy actor who soft-deleted */
    public void setDeletedBy(User deletedBy) {
        this.deletedBy = deletedBy;
    }

    /**
     * Adds a tag to this post if not already present (by normalized name).
     *
     * @param tag tag to add
     */
    public void addTag(Tag tag) {
        if (tag == null || tag.getName() == null || tag.getName().isBlank()) {
            return;
        }
        Tag normalized = Tag.normalize(tag.getName());
        for (Tag existing : tags) {
            if (existing.getName().equalsIgnoreCase(normalized.getName())) {
                return;
            }
        }
        tags.add(normalized);
    }

    /**
     * Removes a tag from this post by normalized name.
     *
     * @param tag tag to remove
     */
    public void removeTag(Tag tag) {
        if (tag == null || tag.getName() == null) {
            return;
        }
        String name = Tag.normalize(tag.getName()).getName();
        tags.removeIf(existing -> existing.getName().equalsIgnoreCase(name));
    }

    /**
     * Computes like count minus dislike count.
     *
     * @return net score (may be negative)
     */
    public int score() {
        return likeCount - dislikeCount;
    }

    /** @return like count */
    int getLikeCount() {
        return likeCount;
    }

    /** @return dislike count */
    int getDislikeCount() {
        return dislikeCount;
    }

    /**
     * Adjusts engagement counters when a like/dislike is added, removed, or switched.
     *
     * @param deltaLikes    change in likes
     * @param deltaDislikes change in dislikes
     */
    void adjustEngagementCounts(int deltaLikes, int deltaDislikes) {
        likeCount += deltaLikes;
        dislikeCount += deltaDislikes;
    }

    /**
     * @return {@code true} if this post is a reply
     */
    public boolean isReply() {
        return parentPost != null;
    }
}
