package voteup.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Period leaderboard (daily, weekly, or monthly). Standings are computed on demand
 * (no cache table in the first design).
 */
public class Leaderboard {

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
     * Calculates current top-ten standings for this period.
     *
     * @return up to ten eligible users ordered by period score
     */
    public List<User> calculateStandings() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Resolves a tie using first-reached-score, then account age.
     *
     * @param u1 first user
     * @param u2 second user
     * @return winning user
     */
    public User resolveTie(User u1, User u2) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Resets the period: awards achievements to current top ten and updates
     * {@link #lastResetTime}.
     */
    public void reset() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
