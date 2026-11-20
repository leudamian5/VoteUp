package voteup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import voteup.model.exception.AuthenticationException;
import voteup.model.exception.BannedException;
import voteup.model.exception.LockoutException;
import voteup.model.exception.UsernameTakenException;
import voteup.model.exception.ValidationException;

/**
 * Process §8 — Auth slice: register, login, progressive lockout, logout.
 */
class AuthTest {

    private VoteUpModel model;
    private final AtomicReference<LocalDateTime> now =
            new AtomicReference<>(LocalDateTime.of(2026, 1, 1, 12, 0));

    @BeforeEach
    void setUp() {
        model = new VoteUpModel();
        now.set(LocalDateTime.of(2026, 1, 1, 12, 0));
        model.setClock(now::get);
    }

    private void advanceMinutes(long minutes) {
        now.set(now.get().plusMinutes(minutes));
    }

    private void advanceHours(long hours) {
        now.set(now.get().plusHours(hours));
    }

    private void failLoginTimes(String username, int times) {
        for (int i = 0; i < times; i++) {
            assertThrows(AuthenticationException.class, () -> model.login(username, "nope"));
        }
    }

    @Test
    void registerSuccess() throws Exception {
        User user = model.register("sam", "password1");
        assertNotNull(user.getUserId());
        assertEquals("sam", user.getUsername());
        assertEquals(Role.USER, user.getRole());
        assertFalse(user.isDeleted());
        assertEquals(0, user.getLockoutTier());
        assertFalse(user.getPasswordHash().equals("password1"));
    }

    @Test
    void registerRejectsBlank() {
        assertThrows(ValidationException.class, () -> model.register("  ", "password1"));
        assertThrows(ValidationException.class, () -> model.register("sam", ""));
    }

    @Test
    void registerRejectsDuplicateActiveUsername() throws Exception {
        model.register("sam", "password1");
        assertThrows(UsernameTakenException.class, () -> model.register("sam", "other"));
    }

    @Test
    void loginSuccessIssuesValidTokenAndClearsFailures() throws Exception {
        model.register("sam", "password1");
        assertThrows(AuthenticationException.class, () -> model.login("sam", "wrong"));
        SessionToken token = model.login("sam", "password1");
        assertTrue(token.isValid(now.get()));
        assertEquals("sam", token.getUser().getUsername());
        assertEquals(0, token.getUser().getFailedLoginAttempts());
        assertEquals(null, token.getUser().getLockoutExpiresAt());
    }

    @Test
    void fiveFailuresCause15MinuteLockAndCorrectPasswordRejected() throws Exception {
        model.register("alex", "secret");
        failLoginTimes("alex", VoteUpModel.FAILED_ATTEMPTS_BEFORE_LOCK);

        assertThrows(LockoutException.class, () -> model.login("alex", "secret"));

        User locked = model.findActiveUserByUsername("alex");
        assertEquals(1, locked.getLockoutTier());
        assertEquals(0, locked.getFailedLoginAttempts());
        assertTrue(locked.isLockedOut(now.get()));
        assertEquals(now.get().plusMinutes(VoteUpModel.LOCKOUT_MINUTES_TIER0),
                locked.getLockoutExpiresAt());
    }

    @Test
    void after15MinuteLockExpiresNextFiveFailuresCause30MinuteLock() throws Exception {
        model.register("alex", "secret");
        failLoginTimes("alex", VoteUpModel.FAILED_ATTEMPTS_BEFORE_LOCK);
        advanceMinutes(VoteUpModel.LOCKOUT_MINUTES_TIER0);
        assertFalse(model.findActiveUserByUsername("alex").isLockedOut(now.get()));

        failLoginTimes("alex", VoteUpModel.FAILED_ATTEMPTS_BEFORE_LOCK);
        assertThrows(LockoutException.class, () -> model.login("alex", "secret"));

        User locked = model.findActiveUserByUsername("alex");
        assertEquals(now.get().plusMinutes(VoteUpModel.LOCKOUT_MINUTES_TIER1),
                locked.getLockoutExpiresAt());
        assertEquals(1, locked.getLockoutTier());
    }

    @Test
    void lockoutProgressionResets24HoursAfterLockEnds() throws Exception {
        model.register("alex", "secret");
        failLoginTimes("alex", VoteUpModel.FAILED_ATTEMPTS_BEFORE_LOCK);
        advanceMinutes(VoteUpModel.LOCKOUT_MINUTES_TIER0);
        advanceHours(VoteUpModel.LOCKOUT_PROGRESSION_RESET_HOURS);

        failLoginTimes("alex", VoteUpModel.FAILED_ATTEMPTS_BEFORE_LOCK);
        assertThrows(LockoutException.class, () -> model.login("alex", "secret"));

        User locked = model.findActiveUserByUsername("alex");
        assertEquals(now.get().plusMinutes(VoteUpModel.LOCKOUT_MINUTES_TIER0),
                locked.getLockoutExpiresAt());
    }

    @Test
    void attemptsDoNotIncrementWhileLocked() throws Exception {
        model.register("alex", "secret");
        failLoginTimes("alex", VoteUpModel.FAILED_ATTEMPTS_BEFORE_LOCK);

        User locked = model.findActiveUserByUsername("alex");
        int attemptsBefore = locked.getFailedLoginAttempts();
        assertThrows(LockoutException.class, () -> model.login("alex", "nope"));
        assertEquals(attemptsBefore, model.findActiveUserByUsername("alex").getFailedLoginAttempts());
    }

    @Test
    void permanentBanBlocksLogin() throws Exception {
        User user = model.register("alex", "secret");
        user.setPermanentlyBanned(true);
        assertThrows(BannedException.class, () -> model.login("alex", "secret"));
    }

    @Test
    void tempBanBlocksLogin() throws Exception {
        User user = model.register("alex", "secret");
        user.setBanExpiresAt(now.get().plusHours(2));
        BannedException ex = assertThrows(BannedException.class, () -> model.login("alex", "secret"));
        assertFalse(ex.isPermanent());
    }

    @Test
    void logoutRevokesSession() throws Exception {
        model.register("sam", "password1");
        SessionToken token = model.login("sam", "password1");
        assertTrue(token.isValid(now.get()));
        model.logout(token.getUser());
        assertFalse(token.isValid(now.get()));
        assertTrue(token.isRevoked());
    }

    @Test
    void unknownUserLoginFailsGenerically() {
        assertThrows(AuthenticationException.class, () -> model.login("nobody", "x"));
    }
}
