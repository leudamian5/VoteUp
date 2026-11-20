package voteup.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Period leaderboard (daily, weekly, or monthly). Standings are computed on demand
 * (no cache table in the first design).
 */
public class Leaderboard {

    /** Maximum standings size. */
    public static final int TOP_TEN_SIZE = 10;

    private String leaderboardId;
    private LeaderboardPeriod period;
    private LocalDateTime lastResetTime;
    private List<User> topTenUsers;

    /**
     * @param leaderboardId unique id
     * @param period        DAILY, WEEKLY, or MONTHLY
     */
    public Leaderboard(String leaderboardId, LeaderboardPeriod period) {
        this.leaderboardId = leaderboardId;
        this.period = period;
        this.lastResetTime = LocalDateTime.now();
        this.topTenUsers = new ArrayList<>();
    }

    /** @return id */
    public String getLeaderboardId() {
        return leaderboardId;
    }

    /** @return period */
    public LeaderboardPeriod getPeriod() {
        return period;
    }

    /** @return last reset time */
    public LocalDateTime getLastResetTime() {
        return lastResetTime;
    }

    /**
     * @param lastResetTime new reset boundary
     */
    public void setLastResetTime(LocalDateTime lastResetTime) {
        this.lastResetTime = lastResetTime;
    }

    /** @return last computed top ten (may be empty before first calculation) */
    public List<User> getTopTenUsers() {
        return topTenUsers;
    }

    /**
     * Returns the last on-demand snapshot filled by {@link VoteUpModel#getStandings}.
     * Standings are not persisted; call the model to recompute.
     *
     * @return up to ten eligible users from the last computation
     */
    public List<User> calculateStandings() {
        return new ArrayList<>(topTenUsers);
    }

    /**
     * Resolves a tie using account age, then user id.
     *
     * @param u1 first user
     * @param u2 second user
     * @return winning user
     */
    public User resolveTie(User u1, User u2) {
        if (u1.getCreatedAt().isBefore(u2.getCreatedAt())) {
            return u1;
        }
        if (u2.getCreatedAt().isBefore(u1.getCreatedAt())) {
            return u2;
        }
        return u1.getUserId().compareTo(u2.getUserId()) <= 0 ? u1 : u2;
    }

    /**
     * Resolves a tie using first-reached-score time, then account age.
     *
     * @param u1      first user
     * @param u2      second user
     * @param reached1 when {@code u1} first reached the tied score
     * @param reached2 when {@code u2} first reached the tied score
     * @return winning user
     */
    User resolveTie(User u1, User u2, LocalDateTime reached1, LocalDateTime reached2) {
        if (reached1 != null && reached2 != null && !reached1.equals(reached2)) {
            return reached1.isBefore(reached2) ? u1 : u2;
        }
        return resolveTie(u1, u2);
    }

    /**
     * Clears the last standings snapshot after a period reset.
     * Achievement awards and {@link #lastResetTime} updates are performed by
     * {@link VoteUpModel#resetLeaderboard}.
     */
    public void reset() {
        topTenUsers.clear();
    }
}
