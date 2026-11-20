package voteup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import voteup.model.exception.AuthenticationException;
import voteup.model.exception.ProfileUnavailableException;
import voteup.model.exception.UserNotFoundException;
import voteup.model.exception.UsernameTakenException;

/**
 * Process §8–9 — Account slice: deleteAccount, updateProfile, getProfile.
 */
class AccountTest {

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
    void deleteAccountRenamesRevokesAndKeepsPosts() throws Exception {
        Post post = model.createPost(alice, "stays", null, null);
        SessionToken token = model.login("alice", "password1");

        model.deleteAccount(alice);

        assertTrue(alice.isDeleted());
        assertEquals("deleted_" + alice.getUserId(), alice.getUsername());
        assertTrue(token.isRevoked());
        assertFalse(post.isDeleted());
        assertEquals(alice, post.getAuthor());
    }

    @Test
    void deletedUsernameBecomesReusable() throws Exception {
        model.deleteAccount(alice);
        User again = model.register("alice", "password1");
        assertNotNull(again.getUserId());
        assertEquals("alice", again.getUsername());
        assertFalse(again.getUserId().equals(alice.getUserId()));
    }

    @Test
    void deleteAccountRejectsUnknownUser() {
        User stranger = new User("x", "stranger", "hash");
        assertThrows(AuthenticationException.class, () -> model.deleteAccount(stranger));
    }

    @Test
    void deleteAccountRejectsAlreadyDeleted() throws Exception {
        model.deleteAccount(alice);
        assertThrows(AuthenticationException.class, () -> model.deleteAccount(alice));
    }

    @Test
    void updateProfileSetsBioAndPicture() throws Exception {
        model.updateProfile(alice, "hello", "img/a.png");
        assertEquals("hello", alice.getBio());
        assertEquals("img/a.png", alice.getProfilePictureUrl());

        model.updateProfile(alice, null, null);
        assertNull(alice.getBio());
        assertNull(alice.getProfilePictureUrl());
    }

    @Test
    void updateProfileRequiresAuthenticatedActiveUser() throws Exception {
        User stranger = new User("x", "stranger", "hash");
        assertThrows(AuthenticationException.class,
                () -> model.updateProfile(stranger, "bio", null));

        model.deleteAccount(alice);
        assertThrows(AuthenticationException.class,
                () -> model.updateProfile(alice, "bio", null));
    }

    @Test
    void getProfileReturnsTarget() throws Exception {
        model.updateProfile(bob, "bob bio", "img/bob.png");
        User profile = model.getProfile(alice, bob);
        assertEquals(bob, profile);
        assertEquals("bob bio", profile.getBio());
    }

    @Test
    void getProfileAllowsViewingDeletedStub() throws Exception {
        model.deleteAccount(bob);
        User profile = model.getProfile(alice, bob);
        assertTrue(profile.isDeleted());
        assertEquals("deleted_" + bob.getUserId(), profile.getUsername());
    }

    @Test
    void getProfileBlockedThrowsUnavailable() throws Exception {
        model.blockUser(alice, bob);
        assertThrows(ProfileUnavailableException.class, () -> model.getProfile(alice, bob));
        assertThrows(ProfileUnavailableException.class, () -> model.getProfile(bob, alice));
    }

    @Test
    void getProfileMissingTarget() {
        User ghost = new User("nope", "ghost", "hash");
        assertThrows(UserNotFoundException.class, () -> model.getProfile(alice, ghost));
    }

    @Test
    void getProfileSelfWorksEvenWithoutFollow() throws Exception {
        User profile = model.getProfile(alice, alice);
        assertEquals(alice, profile);
    }

    @Test
    void registerStillRejectsActiveDuplicate() throws Exception {
        assertThrows(UsernameTakenException.class, () -> model.register("alice", "other"));
    }
}
