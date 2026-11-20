package voteup.model;

import java.time.LocalDateTime;

/**
 * Record that a user finished in the top 10 of a leaderboard at a reset event.
 * Multiple achievements per period are allowed across resets.
 */
public class Achievement {

    private String achievementId;
    private String title;
    private LocalDateTime dateEarned;
    private LeaderboardPeriod leaderboardPeriod;
    private User user;

    /**
     * @param achievementId unique id
     * @param user          recipient
     * @param title         display title
     * @param period        leaderboard period
     */
    public Achievement(String achievementId, User user, String title, LeaderboardPeriod period) {
        this.achievementId = achievementId;
        this.user = user;
        this.title = title;
        this.leaderboardPeriod = period;
        this.dateEarned = LocalDateTime.now();
    }

    /** @return achievement id */
    public String getAchievementId() {
        return achievementId;
    }

    /** @return title */
    public String getTitle() {
        return title;
    }

    /** @return when earned */
    public LocalDateTime getDateEarned() {
        return dateEarned;
    }

    /**
     * @param dateEarned when earned (model clock / tests)
     */
    void setDateEarned(LocalDateTime dateEarned) {
        this.dateEarned = dateEarned;
    }

    /** @return period */
    public LeaderboardPeriod getLeaderboardPeriod() {
        return leaderboardPeriod;
    }

    /** @return recipient */
    public User getUser() {
        return user;
    }
}
