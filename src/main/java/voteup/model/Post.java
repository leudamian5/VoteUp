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
     * Adds a tag to this post.
     *
     * @param tag tag to add
     */
    public void addTag(Tag tag) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Removes a tag from this post.
     *
     * @param tag tag to remove
     */
    public void removeTag(Tag tag) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Computes like count minus dislike count.
     *
     * @return net score (may be negative)
     */
    public int score() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * @return {@code true} if this post is a reply
     */
    public boolean isReply() {
        return parentPost != null;
    }
}
