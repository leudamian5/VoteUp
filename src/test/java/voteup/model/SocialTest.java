package voteup.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import voteup.model.exception.AuthenticationException;
import voteup.model.exception.SocialException;
import voteup.model.exception.UserNotFoundException;

/**
 * Process §8–9 — Social slice: follow/unfollow/block/unblock + visibility helpers.
 */
class SocialTest {

    private VoteUpModel model;
    private final AtomicReference<LocalDateTime> now =
            new AtomicReference<>(LocalDateTime.of(2026, 1, 1, 12, 0));

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() throws Exception {
        model = new VoteUpModel();
        now.set(LocalDateTime.of(2026, 1, 1, 12, 0));
        model.setClock(now::get);
        alice = model.register("alice", "password1");
        bob = model.register("bob", "password1");
    }

    @Test
    void followSucceeds() throws Exception {
        model.followUser(alice, bob);
        assertTrue(model.isFollowing(alice, bob));
        assertFalse(model.isFollowing(bob, alice));
    }

    @Test
    void unfollowSucceeds() throws Exception {
        model.followUser(alice, bob);
        model.unfollowUser(alice, bob);
        assertFalse(model.isFollowing(alice, bob));
    }

    @Test
    void cannotFollowSelf() {
        assertThrows(SocialException.class, () -> model.followUser(alice, alice));
    }

    @Test
    void cannotFollowTwice() throws Exception {
        model.followUser(alice, bob);
        assertThrows(SocialException.class, () -> model.followUser(alice, bob));
    }

    @Test
    void unfollowWhenNotFollowingFails() {
        assertThrows(SocialException.class, () -> model.unfollowUser(alice, bob));
    }

    @Test
    void followRejectsMissingTarget() {
        User ghost = new User("x", "ghost", "hash");
        assertThrows(UserNotFoundException.class, () -> model.followUser(alice, ghost));
    }

    @Test
    void followRejectsUnknownActor() {
        User stranger = new User("x", "stranger", "hash");
        assertThrows(AuthenticationException.class, () -> model.followUser(stranger, bob));
    }

    @Test
    void blockSucceedsAndPreventsFollow() throws Exception {
        model.blockUser(alice, bob);
        assertTrue(model.isBlocked(alice, bob));
        assertTrue(model.hasBlockBetween(alice, bob));
        assertFalse(model.canViewContent(bob, alice));
        assertFalse(model.canViewContent(alice, bob));

        assertThrows(SocialException.class, () -> model.followUser(bob, alice));
        assertThrows(SocialException.class, () -> model.followUser(alice, bob));
    }

    @Test
    void blockRemovesFollowsBothDirections() throws Exception {
        model.followUser(alice, bob);
        model.followUser(bob, alice);
        model.blockUser(alice, bob);

        assertFalse(model.isFollowing(alice, bob));
        assertFalse(model.isFollowing(bob, alice));
        assertTrue(model.isBlocked(alice, bob));
    }

    @Test
    void cannotBlockSelf() {
        assertThrows(SocialException.class, () -> model.blockUser(alice, alice));
    }

    @Test
    void cannotBlockTwice() throws Exception {
        model.blockUser(alice, bob);
        assertThrows(SocialException.class, () -> model.blockUser(alice, bob));
    }

    @Test
    void unblockSucceeds() throws Exception {
        model.blockUser(alice, bob);
        model.unblockUser(alice, bob);
        assertFalse(model.isBlocked(alice, bob));
        assertTrue(model.canViewContent(alice, bob));
        model.followUser(bob, alice);
        assertTrue(model.isFollowing(bob, alice));
    }

    @Test
    void unblockWhenNotBlockedFails() {
        assertThrows(SocialException.class, () -> model.unblockUser(alice, bob));
    }

    @Test
    void unblockDoesNotRestoreFollows() throws Exception {
        model.followUser(alice, bob);
        model.blockUser(alice, bob);
        model.unblockUser(alice, bob);
        assertFalse(model.isFollowing(alice, bob));
    }

    @Test
    void deletedTargetIsNotFound() throws Exception {
        bob.setDeleted(true);
        assertThrows(UserNotFoundException.class, () -> model.followUser(alice, bob));
        assertThrows(UserNotFoundException.class, () -> model.blockUser(alice, bob));
    }
}
