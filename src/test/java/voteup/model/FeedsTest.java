package voteup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import voteup.model.exception.AuthenticationException;
import voteup.model.exception.PostNotFoundException;
import voteup.model.exception.ValidationException;

/**
 * Process §8–9 — Feeds slice: main/following/tag feeds, post+replies, search, paging, tie-break.
 */
class FeedsTest {

    private VoteUpModel model;
    private final AtomicReference<LocalDateTime> now =
            new AtomicReference<>(LocalDateTime.of(2026, 1, 1, 12, 0));

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() throws Exception {
        model = new VoteUpModel();
        now.set(LocalDateTime.of(2026, 1, 1, 12, 0));
        model.setClock(now::get);

        alice = model.register("alice", "password1");
        bob = model.register("bob", "password1");
        carol = model.register("carol", "password1");
        alice.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        bob.setCreatedAt(LocalDateTime.of(2026, 1, 1, 11, 0));
        carol.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    private void advanceMinutes(long minutes) {
        now.set(now.get().plusMinutes(minutes));
    }

    @Test
    void mainFeedExcludesRepliesDeletedAndBlocked() throws Exception {
        Post p1 = model.createPost(alice, "a1", null, null);
        advanceMinutes(1);
        Post replyParent = model.createPost(bob, "b1", null, null);
        advanceMinutes(1);
        model.createReply(carol, replyParent, "reply", null);
        advanceMinutes(1);
        Post deleted = model.createPost(carol, "c-del", null, null);
        model.deletePost(carol, deleted);
        advanceMinutes(1);
        Post blockedAuthorPost = model.createPost(bob, "b2", null, null);
        model.blockUser(alice, bob);

        List<Post> feed = model.getMainFeed(alice, 0, VoteUpModel.FEED_PAGE_LIMIT);
        assertEquals(List.of(p1), feed);
        assertFalse(feed.contains(replyParent));
        assertFalse(feed.contains(blockedAuthorPost));
    }

    @Test
    void mainFeedNewestFirstWithTieBreak() throws Exception {
        LocalDateTime t = now.get();
        Post older = model.createPost(bob, "older", null, null);
        older.setCreatedAt(t);
        Post newerSameAuthorTime = model.createPost(alice, "newer-author-older", null, null);
        newerSameAuthorTime.setCreatedAt(t);
        Post newer = model.createPost(carol, "newest", null, null);
        newer.setCreatedAt(t.plusMinutes(1));

        // Same createdAt: author.createdAt ASC → alice before bob; then post_id ASC as last key
        List<Post> feed = model.getMainFeed(alice, 0, VoteUpModel.FEED_PAGE_LIMIT);
        assertEquals(newer, feed.get(0));
        assertEquals(newerSameAuthorTime, feed.get(1));
        assertEquals(older, feed.get(2));
    }

    @Test
    void mainFeedPagingAndValidation() throws Exception {
        for (int i = 0; i < 3; i++) {
            advanceMinutes(1);
            model.createPost(alice, "p" + i, null, null);
        }
        List<Post> page = model.getMainFeed(alice, 1, 1);
        assertEquals(1, page.size());

        assertThrows(ValidationException.class, () -> model.getMainFeed(alice, -1, 10));
        assertThrows(ValidationException.class, () -> model.getMainFeed(alice, 0, 0));
        assertThrows(ValidationException.class,
                () -> model.getMainFeed(alice, 0, VoteUpModel.FEED_PAGE_LIMIT + 1));
    }

    @Test
    void followingFeedOnlyFolloweesTopLevel() throws Exception {
        model.followUser(alice, bob);
        Post fromBob = model.createPost(bob, "bob-post", null, null);
        advanceMinutes(1);
        model.createPost(carol, "carol-post", null, null);
        advanceMinutes(1);
        model.createReply(bob, fromBob, "bob-reply", null);

        List<Post> feed = model.getFollowingFeed(alice, 0, VoteUpModel.FEED_PAGE_LIMIT);
        assertEquals(List.of(fromBob), feed);
    }

    @Test
    void followingFeedRequiresAuth() {
        User stranger = new User("x", "stranger", "hash");
        assertThrows(AuthenticationException.class,
                () -> model.getFollowingFeed(stranger, 0, 10));
    }

    @Test
    void tagFeedFiltersByNormalizedTag() throws Exception {
        Post tagged = model.createPost(alice, "java post", null, List.of(new Tag("#Java")));
        advanceMinutes(1);
        model.createPost(bob, "other", null, List.of(new Tag("python")));

        List<Post> feed = model.getTagFeed(alice, new Tag("JAVA"), 0, VoteUpModel.FEED_PAGE_LIMIT);
        assertEquals(List.of(tagged), feed);
        assertThrows(ValidationException.class,
                () -> model.getTagFeed(alice, null, 0, 10));
    }

    @Test
    void getPostWithRepliesReturnsRootThenVisibleReplies() throws Exception {
        Post root = model.createPost(alice, "root", null, null);
        advanceMinutes(1);
        Post r1 = model.createReply(bob, root, "r1", null);
        advanceMinutes(1);
        Post r2 = model.createReply(carol, root, "r2", null);
        model.deletePost(carol, r2);

        List<Post> page = model.getPostWithReplies(alice, root);
        assertEquals(List.of(root, r1), page);
    }

    @Test
    void getPostWithRepliesUnavailableWhenBlockedOrMissing() throws Exception {
        Post root = model.createPost(bob, "root", null, null);
        model.blockUser(alice, bob);
        assertThrows(PostNotFoundException.class, () -> model.getPostWithReplies(alice, root));

        Post missing = new Post("nope", alice, "ghost", null);
        assertThrows(PostNotFoundException.class, () -> model.getPostWithReplies(alice, missing));
    }

    @Test
    void searchUsersMatchesUsernameAndRespectsBlocks() throws Exception {
        model.blockUser(alice, bob);
        List<User> found = model.searchUsers(alice, "a");
        assertTrue(found.stream().anyMatch(u -> u.getUsername().equals("alice")));
        assertTrue(found.stream().anyMatch(u -> u.getUsername().equals("carol")));
        assertFalse(found.stream().anyMatch(u -> u.getUsername().equals("bob")));

        assertThrows(ValidationException.class, () -> model.searchUsers(alice, "  "));
        User stranger = new User("x", "stranger", "hash");
        assertThrows(AuthenticationException.class, () -> model.searchUsers(stranger, "a"));
    }

    @Test
    void searchByTagFindsPostsAndCapsAtPageLimit() throws Exception {
        Post p = model.createPost(alice, "t", null, List.of(new Tag("mvc")));
        List<Post> found = model.searchByTag(alice, "#MVC");
        assertEquals(List.of(p), found);

        assertThrows(ValidationException.class, () -> model.searchByTag(alice, ""));
    }
}
