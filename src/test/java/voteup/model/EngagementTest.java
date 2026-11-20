package voteup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import voteup.model.exception.AuthenticationException;
import voteup.model.exception.EngagementException;
import voteup.model.exception.PostNotFoundException;

/**
 * Process §8–9 — Engagement slice: like/dislike upsert, switch resets createdAt, remove, score.
 */
class EngagementTest {

    private VoteUpModel model;
    private final AtomicReference<LocalDateTime> now =
            new AtomicReference<>(LocalDateTime.of(2026, 1, 1, 12, 0));

    private User alice;
    private User bob;
    private Post post;

    @BeforeEach
    void setUp() throws Exception {
        model = new VoteUpModel();
        now.set(LocalDateTime.of(2026, 1, 1, 12, 0));
        model.setClock(now::get);
        alice = model.register("alice", "password1");
        bob = model.register("bob", "password1");
        post = model.createPost(alice, "hello", null, null);
    }

    private void advanceMinutes(long minutes) {
        now.set(now.get().plusMinutes(minutes));
    }

    @Test
    void likeIncreasesScore() throws Exception {
        model.applyEngagement(bob, post, EngagementType.LIKE);
        assertEquals(1, post.score());
        Engagement e = model.findEngagement(bob, post);
        assertNotNull(e);
        assertEquals(EngagementType.LIKE, e.getType());
        assertEquals(now.get(), e.getCreatedAt());
    }

    @Test
    void dislikeDecreasesScore() throws Exception {
        model.applyEngagement(bob, post, EngagementType.DISLIKE);
        assertEquals(-1, post.score());
    }

    @Test
    void scoreIsLikesMinusDislikes() throws Exception {
        User carol = model.register("carol", "password1");
        model.applyEngagement(bob, post, EngagementType.LIKE);
        model.applyEngagement(carol, post, EngagementType.LIKE);
        model.applyEngagement(alice, post, EngagementType.DISLIKE);
        assertEquals(1, post.score());
    }

    @Test
    void oneEngagementRowPerUserPost() throws Exception {
        model.applyEngagement(bob, post, EngagementType.LIKE);
        model.applyEngagement(bob, post, EngagementType.LIKE);
        assertEquals(1, post.score());
        assertEquals(1, model.countEngagementsFor(post));
    }

    @Test
    void switchLikeToDislikeResetsCreatedAtAndKeepsOneRow() throws Exception {
        model.applyEngagement(bob, post, EngagementType.LIKE);
        LocalDateTime firstAt = model.findEngagement(bob, post).getCreatedAt();
        advanceMinutes(10);

        model.applyEngagement(bob, post, EngagementType.DISLIKE);

        Engagement e = model.findEngagement(bob, post);
        assertEquals(EngagementType.DISLIKE, e.getType());
        assertEquals(now.get(), e.getCreatedAt());
        assertTrue(e.getCreatedAt().isAfter(firstAt));
        assertEquals(1, model.countEngagementsFor(post));
        assertEquals(-1, post.score());
    }

    @Test
    void switchDislikeToLikeResetsCreatedAt() throws Exception {
        model.applyEngagement(bob, post, EngagementType.DISLIKE);
        advanceMinutes(5);
        model.applyEngagement(bob, post, EngagementType.LIKE);
        Engagement e = model.findEngagement(bob, post);
        assertEquals(EngagementType.LIKE, e.getType());
        assertEquals(now.get(), e.getCreatedAt());
        assertEquals(1, post.score());
    }

    @Test
    void removeEngagementClearsRowAndScore() throws Exception {
        model.applyEngagement(bob, post, EngagementType.LIKE);
        model.removeEngagement(bob, post);
        assertNull(model.findEngagement(bob, post));
        assertEquals(0, post.score());
    }

    @Test
    void removeWhenNoneThrows() {
        assertThrows(EngagementException.class, () -> model.removeEngagement(bob, post));
    }

    @Test
    void applyRejectsNullType() {
        assertThrows(EngagementException.class,
                () -> model.applyEngagement(bob, post, null));
    }

    @Test
    void applyRejectsMissingOrDeletedPost() throws Exception {
        Post missing = new Post("nope", alice, "ghost", null);
        assertThrows(PostNotFoundException.class,
                () -> model.applyEngagement(bob, missing, EngagementType.LIKE));

        model.deletePost(alice, post);
        assertThrows(PostNotFoundException.class,
                () -> model.applyEngagement(bob, post, EngagementType.LIKE));
    }

    @Test
    void removeRejectsMissingPost() {
        Post missing = new Post("nope", alice, "ghost", null);
        assertThrows(PostNotFoundException.class,
                () -> model.removeEngagement(bob, missing));
    }

    @Test
    void unknownUserCannotEngage() {
        User stranger = new User("x", "stranger", "hash");
        assertThrows(AuthenticationException.class,
                () -> model.applyEngagement(stranger, post, EngagementType.LIKE));
        assertThrows(AuthenticationException.class,
                () -> model.removeEngagement(stranger, post));
    }

    @Test
    void canEngageOnReply() throws Exception {
        Post reply = model.createReply(bob, post, "reply", null);
        model.applyEngagement(alice, reply, EngagementType.LIKE);
        assertEquals(1, reply.score());
    }
}
