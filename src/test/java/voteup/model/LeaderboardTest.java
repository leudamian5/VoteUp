package voteup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Process §8–9 — Leaderboard slice: standings, reset, achievements, firstReachedAt ties.
 */
class LeaderboardTest {

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
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            model.getLeaderboard(period).setLastResetTime(now.get());
        }

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
    void standingsRankByPeriodNetScore() throws Exception {
        Post alicePost = model.createPost(alice, "a", null, null);
        Post bobPost = model.createPost(bob, "b", null, null);
        advanceMinutes(1);
        model.applyEngagement(bob, alicePost, EngagementType.LIKE);
        model.applyEngagement(carol, alicePost, EngagementType.LIKE);
        advanceMinutes(1);
        model.applyEngagement(alice, bobPost, EngagementType.LIKE);

        List<User> standings = model.getStandings(LeaderboardPeriod.DAILY);
        assertEquals(List.of(alice, bob), standings);
    }

    @Test
    void standingsExcludeDeletedAndPermBanned() throws Exception {
        Post alicePost = model.createPost(alice, "a", null, null);
        Post bobPost = model.createPost(bob, "b", null, null);
        advanceMinutes(1);
        model.applyEngagement(carol, alicePost, EngagementType.LIKE);
        model.applyEngagement(carol, bobPost, EngagementType.LIKE);

        bob.setPermanentlyBanned(true);
        alice.setDeleted(true);

        List<User> standings = model.getStandings(LeaderboardPeriod.DAILY);
        assertTrue(standings.isEmpty());
    }

    @Test
    void equalScoreUsesFirstReachedAtThenAccountAge() throws Exception {
        Post alicePost = model.createPost(alice, "a", null, null);
        Post bobPost = model.createPost(bob, "b", null, null);

        advanceMinutes(1);
        model.applyEngagement(carol, bobPost, EngagementType.LIKE);
        advanceMinutes(5);
        model.applyEngagement(carol, alicePost, EngagementType.LIKE);

        // Both score 1; bob reached first → bob ranks higher despite younger account
        List<User> standings = model.getStandings(LeaderboardPeriod.DAILY);
        assertEquals(List.of(bob, alice), standings);
    }

    @Test
    void topTenOnlyAndTenthPlaceTieUsesFirstReached() throws Exception {
        List<User> authors = new ArrayList<>();
        authors.add(alice);
        authors.add(bob);
        authors.add(carol);
        for (int i = 0; i < 9; i++) {
            User u = model.register("u" + i, "password1");
            u.setCreatedAt(LocalDateTime.of(2026, 1, 1, 13, i));
            authors.add(u);
        }
        // 12 authors total; give each score 1, with staggered firstReachedAt
        for (int i = 0; i < authors.size(); i++) {
            User author = authors.get(i);
            Post post = model.createPost(author, "p" + i, null, null);
            advanceMinutes(1);
            User liker = authors.get((i + 1) % authors.size());
            model.applyEngagement(liker, post, EngagementType.LIKE);
        }

        List<User> standings = model.getStandings(LeaderboardPeriod.DAILY);
        assertEquals(Leaderboard.TOP_TEN_SIZE, standings.size());
        // Earliest firstReachedAt wins ties — alice was liked first in the loop
        assertEquals(alice, standings.get(0));
        assertFalse(standings.contains(authors.get(authors.size() - 1)));
        assertFalse(standings.contains(authors.get(authors.size() - 2)));
    }

    @Test
    void resetAwardsAchievementsAndStartsNewPeriod() throws Exception {
        Post alicePost = model.createPost(alice, "a", null, null);
        advanceMinutes(1);
        model.applyEngagement(bob, alicePost, EngagementType.LIKE);

        model.resetLeaderboard(LeaderboardPeriod.DAILY);

        assertEquals(1, alice.getAchievements().size());
        Achievement a = alice.getAchievements().get(0);
        assertEquals(LeaderboardPeriod.DAILY, a.getLeaderboardPeriod());
        assertEquals(now.get(), a.getDateEarned());
        assertTrue(a.getTitle().contains("DAILY"));

        assertTrue(model.getStandings(LeaderboardPeriod.DAILY).isEmpty());

        advanceMinutes(1);
        model.applyEngagement(carol, alicePost, EngagementType.LIKE);
        assertEquals(List.of(alice), model.getStandings(LeaderboardPeriod.DAILY));

        model.resetLeaderboard(LeaderboardPeriod.DAILY);
        assertEquals(2, alice.getAchievements().size());
    }

    @Test
    void periodsAreIndependent() throws Exception {
        Post alicePost = model.createPost(alice, "a", null, null);
        advanceMinutes(1);
        model.applyEngagement(bob, alicePost, EngagementType.LIKE);

        model.resetLeaderboard(LeaderboardPeriod.DAILY);
        assertEquals(1, alice.getAchievements().size());
        assertEquals(List.of(alice), model.getStandings(LeaderboardPeriod.WEEKLY));
    }

    @Test
    void dislikeLowersPeriodScore() throws Exception {
        Post alicePost = model.createPost(alice, "a", null, null);
        Post bobPost = model.createPost(bob, "b", null, null);
        advanceMinutes(1);
        model.applyEngagement(carol, alicePost, EngagementType.LIKE);
        model.applyEngagement(carol, bobPost, EngagementType.DISLIKE);

        List<User> standings = model.getStandings(LeaderboardPeriod.DAILY);
        assertEquals(alice, standings.get(0));
        assertTrue(standings.contains(bob));
        assertEquals(bob, standings.get(standings.size() - 1));
    }
}
