package voteup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import voteup.model.exception.AuthorizationException;
import voteup.model.exception.BannedException;
import voteup.model.exception.ModerationException;
import voteup.model.exception.ValidationException;

/**
 * Process §8–9 — Moderation slice: temp/perm ban, promote/demote, session revoke, mod delete.
 */
class ModerationTest {

    private VoteUpModel model;
    private final AtomicReference<LocalDateTime> now =
            new AtomicReference<>(LocalDateTime.of(2026, 1, 1, 12, 0));

    private User admin;
    private User mod;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        model = new VoteUpModel();
        now.set(LocalDateTime.of(2026, 1, 1, 12, 0));
        model.setClock(now::get);

        admin = model.register("admin", "password1");
        mod = model.register("mod", "password1");
        user = model.register("user", "password1");
        admin.setRole(Role.ADMIN);
        mod.setRole(Role.MOD);
    }

    @Test
    void tempBanSetsExpiryRevokesSessionAndBlocksLogin() throws Exception {
        SessionToken token = model.login("user", "password1");
        assertTrue(token.isValid(now.get()));

        model.issueTempBan(mod, user, 2);

        assertEquals(now.get().plusHours(2), user.getBanExpiresAt());
        assertFalse(user.isPermanentlyBanned());
        assertEquals("user", user.getUsername());
        assertTrue(token.isRevoked());
        assertThrows(BannedException.class, () -> model.login("user", "password1"));
    }

    @Test
    void tempBanExpiresAndLoginWorksAgain() throws Exception {
        model.issueTempBan(mod, user, 1);
        now.set(now.get().plusHours(1));
        assertFalse(user.isTempBanned(now.get()));
        SessionToken token = model.login("user", "password1");
        assertTrue(token.isValid(now.get()));
    }

    @Test
    void permBanKeepsUsernameAndPosts() throws Exception {
        Post post = model.createPost(user, "keep me", null, null);
        SessionToken token = model.login("user", "password1");

        model.issuePermBan(admin, user);

        assertTrue(user.isPermanentlyBanned());
        assertEquals("user", user.getUsername());
        assertFalse(user.isDeleted());
        assertFalse(post.isDeleted());
        assertTrue(token.isRevoked());
        assertThrows(BannedException.class, () -> model.login("user", "password1"));
    }

    @Test
    void regularUserCannotBan() {
        assertThrows(AuthorizationException.class, () -> model.issueTempBan(user, mod, 1));
        assertThrows(AuthorizationException.class, () -> model.issuePermBan(user, mod));
    }

    @Test
    void cannotBanSelfOrAdmin() {
        assertThrows(ModerationException.class, () -> model.issueTempBan(mod, mod, 1));
        assertThrows(ModerationException.class, () -> model.issuePermBan(admin, admin));
        assertThrows(ModerationException.class, () -> model.issueTempBan(mod, admin, 1));
    }

    @Test
    void modCannotBanAnotherMod() throws Exception {
        User otherMod = model.register("mod2", "password1");
        otherMod.setRole(Role.MOD);
        assertThrows(ModerationException.class, () -> model.issuePermBan(mod, otherMod));
        assertThrows(ModerationException.class, () -> model.issueTempBan(mod, otherMod, 1));
    }

    @Test
    void adminCanBanMod() throws Exception {
        model.issuePermBan(admin, mod);
        assertTrue(mod.isPermanentlyBanned());
    }

    @Test
    void tempBanRejectsInvalidDuration() {
        assertThrows(ValidationException.class, () -> model.issueTempBan(mod, user, 0));
        assertThrows(ValidationException.class, () -> model.issueTempBan(mod, user, -3));
    }

    @Test
    void promoteAndDemoteRequireAdmin() throws Exception {
        assertThrows(AuthorizationException.class, () -> model.promoteToMod(mod, user));
        model.promoteToMod(admin, user);
        assertEquals(Role.MOD, user.getRole());

        assertThrows(AuthorizationException.class, () -> model.demoteFromMod(mod, user));
        model.demoteFromMod(admin, user);
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void promoteRejectsNonUserTarget() {
        assertThrows(ModerationException.class, () -> model.promoteToMod(admin, mod));
        assertThrows(ModerationException.class, () -> model.promoteToMod(admin, admin));
    }

    @Test
    void demoteRejectsNonModTarget() {
        assertThrows(ModerationException.class, () -> model.demoteFromMod(admin, user));
        assertThrows(ModerationException.class, () -> model.demoteFromMod(admin, admin));
    }

    @Test
    void promotedModCanSoftDeleteOthersPost() throws Exception {
        Post post = model.createPost(user, "spam", null, null);
        model.promoteToMod(admin, user);
        // user is now MOD — use a fresh USER author
        User author = model.register("author", "password1");
        Post target = model.createPost(author, "remove me", null, null);

        model.deletePost(user, target);
        assertTrue(target.isDeleted());
        assertEquals(user, target.getDeletedBy());
        assertFalse(post.isDeleted());
    }

    @Test
    void cannotPermBanAlreadyPermBanned() throws Exception {
        model.issuePermBan(admin, user);
        assertThrows(ModerationException.class, () -> model.issuePermBan(admin, user));
    }
}
