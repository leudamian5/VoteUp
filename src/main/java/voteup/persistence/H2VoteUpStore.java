package voteup.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import voteup.model.Achievement;
import voteup.model.Block;
import voteup.model.Engagement;
import voteup.model.EngagementType;
import voteup.model.Follow;
import voteup.model.Leaderboard;
import voteup.model.LeaderboardPeriod;
import voteup.model.Post;
import voteup.model.Role;
import voteup.model.Tag;
import voteup.model.User;
import voteup.model.VoteUpModel;
import voteup.model.exception.StorageException;

/**
 * H2 file-mode snapshot store for {@link VoteUpModel}. Uses AUTO_SERVER so multiple
 * local JVMs can share the same database file.
 */
public class H2VoteUpStore {

    /** Default relative DB path under the VoteUp working directory. */
    public static final String DEFAULT_DB_PATH = "./data/voteup";

    private final String jdbcUrl;

    /**
     * Opens (or creates) the default file database with AUTO_SERVER enabled.
     *
     * @throws StorageException if the data directory cannot be created
     */
    public H2VoteUpStore() throws StorageException {
        this(DEFAULT_DB_PATH);
    }

    /**
     * @param filePath H2 file path without {@code .mv.db} suffix (e.g. {@code ./data/voteup})
     * @throws StorageException if the parent directory cannot be created
     */
    public H2VoteUpStore(String filePath) throws StorageException {
        try {
            Path parent = Path.of(filePath).toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception ex) {
            throw new StorageException("Could not create H2 data directory", ex);
        }
        this.jdbcUrl = "jdbc:h2:file:" + filePath + ";AUTO_SERVER=TRUE";
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    /**
     * Creates tables if they do not exist.
     *
     * @throws StorageException on SQL failure
     */
    public void ensureSchema() throws StorageException {
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                      user_id VARCHAR(64) PRIMARY KEY,
                      username VARCHAR(128) NOT NULL,
                      password_hash VARCHAR(128) NOT NULL,
                      bio VARCHAR(4000),
                      profile_picture_url VARCHAR(2000),
                      role VARCHAR(16) NOT NULL,
                      is_deleted BOOLEAN NOT NULL,
                      is_permanently_banned BOOLEAN NOT NULL,
                      ban_expires_at TIMESTAMP,
                      failed_login_attempts INT NOT NULL,
                      lockout_expires_at TIMESTAMP,
                      last_failed_login TIMESTAMP,
                      lockout_tier INT NOT NULL,
                      last_lock_ended_at TIMESTAMP,
                      created_at TIMESTAMP NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS posts (
                      post_id VARCHAR(64) PRIMARY KEY,
                      author_id VARCHAR(64) NOT NULL,
                      text_content VARCHAR(8000),
                      image_url VARCHAR(2000),
                      created_at TIMESTAMP NOT NULL,
                      parent_post_id VARCHAR(64),
                      is_deleted BOOLEAN NOT NULL,
                      deleted_by_id VARCHAR(64),
                      like_count INT NOT NULL,
                      dislike_count INT NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS post_tags (
                      post_id VARCHAR(64) NOT NULL,
                      tag_name VARCHAR(128) NOT NULL,
                      PRIMARY KEY (post_id, tag_name)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS engagements (
                      engagement_id VARCHAR(64) PRIMARY KEY,
                      user_id VARCHAR(64) NOT NULL,
                      post_id VARCHAR(64) NOT NULL,
                      type VARCHAR(16) NOT NULL,
                      created_at TIMESTAMP NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS follows (
                      follower_id VARCHAR(64) NOT NULL,
                      followee_id VARCHAR(64) NOT NULL,
                      created_at TIMESTAMP NOT NULL,
                      PRIMARY KEY (follower_id, followee_id)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS blocks (
                      blocker_id VARCHAR(64) NOT NULL,
                      blocked_id VARCHAR(64) NOT NULL,
                      created_at TIMESTAMP NOT NULL,
                      PRIMARY KEY (blocker_id, blocked_id)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS achievements (
                      achievement_id VARCHAR(64) PRIMARY KEY,
                      user_id VARCHAR(64) NOT NULL,
                      title VARCHAR(256) NOT NULL,
                      period VARCHAR(16) NOT NULL,
                      date_earned TIMESTAMP NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS leaderboards (
                      period VARCHAR(16) PRIMARY KEY,
                      leaderboard_id VARCHAR(64) NOT NULL,
                      last_reset_time TIMESTAMP NOT NULL
                    )
                    """);
        } catch (SQLException ex) {
            throw new StorageException("Failed to ensure H2 schema", ex);
        }
    }

    /**
     * @return {@code true} if no users are stored
     * @throws StorageException on SQL failure
     */
    public boolean isEmpty() throws StorageException {
        try (Connection conn = connect();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getInt(1) == 0;
        } catch (SQLException ex) {
            throw new StorageException("Failed to check whether H2 store is empty", ex);
        }
    }

    /**
     * Replaces all persisted rows with a snapshot of the model (sessions excluded).
     *
     * @param model source model
     * @throws StorageException on SQL failure
     */
    public void save(VoteUpModel model) throws StorageException {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM achievements");
                st.executeUpdate("DELETE FROM post_tags");
                st.executeUpdate("DELETE FROM engagements");
                st.executeUpdate("DELETE FROM follows");
                st.executeUpdate("DELETE FROM blocks");
                st.executeUpdate("DELETE FROM posts");
                st.executeUpdate("DELETE FROM users");
                st.executeUpdate("DELETE FROM leaderboards");
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO users (
                      user_id, username, password_hash, bio, profile_picture_url, role,
                      is_deleted, is_permanently_banned, ban_expires_at, failed_login_attempts,
                      lockout_expires_at, last_failed_login, lockout_tier, last_lock_ended_at, created_at
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """)) {
                for (User user : model.getUsersSnapshot()) {
                    ps.setString(1, user.getUserId());
                    ps.setString(2, user.getUsername());
                    ps.setString(3, user.getPasswordHash());
                    ps.setString(4, user.getBio());
                    ps.setString(5, user.getProfilePictureUrl());
                    ps.setString(6, user.getRole().name());
                    ps.setBoolean(7, user.isDeleted());
                    ps.setBoolean(8, user.isPermanentlyBanned());
                    setTimestamp(ps, 9, user.getBanExpiresAt());
                    ps.setInt(10, user.getFailedLoginAttempts());
                    setTimestamp(ps, 11, user.getLockoutExpiresAt());
                    setTimestamp(ps, 12, user.getLastFailedLogin());
                    ps.setInt(13, user.getLockoutTier());
                    setTimestamp(ps, 14, user.getLastLockEndedAt());
                    setTimestamp(ps, 15, user.getCreatedAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO posts (
                      post_id, author_id, text_content, image_url, created_at, parent_post_id,
                      is_deleted, deleted_by_id, like_count, dislike_count
                    ) VALUES (?,?,?,?,?,?,?,?,?,?)
                    """)) {
                for (Post post : model.getPostsSnapshot()) {
                    ps.setString(1, post.getPostId());
                    ps.setString(2, post.getAuthor().getUserId());
                    ps.setString(3, post.getTextContent());
                    ps.setString(4, post.getImageUrl());
                    setTimestamp(ps, 5, post.getCreatedAt());
                    if (post.getParentPost() == null) {
                        ps.setNull(6, Types.VARCHAR);
                    } else {
                        ps.setString(6, post.getParentPost().getPostId());
                    }
                    ps.setBoolean(7, post.isDeleted());
                    if (post.getDeletedBy() == null) {
                        ps.setNull(8, Types.VARCHAR);
                    } else {
                        ps.setString(8, post.getDeletedBy().getUserId());
                    }
                    ps.setInt(9, post.getLikeCount());
                    ps.setInt(10, post.getDislikeCount());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO post_tags (post_id, tag_name) VALUES (?,?)")) {
                for (Post post : model.getPostsSnapshot()) {
                    for (Tag tag : post.getTags()) {
                        ps.setString(1, post.getPostId());
                        ps.setString(2, tag.getName());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO engagements (engagement_id, user_id, post_id, type, created_at)
                    VALUES (?,?,?,?,?)
                    """)) {
                for (Engagement engagement : model.getEngagementsSnapshot()) {
                    ps.setString(1, engagement.getEngagementId());
                    ps.setString(2, engagement.getUser().getUserId());
                    ps.setString(3, engagement.getPost().getPostId());
                    ps.setString(4, engagement.getType().name());
                    setTimestamp(ps, 5, engagement.getCreatedAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO follows (follower_id, followee_id, created_at) VALUES (?,?,?)")) {
                for (Follow follow : model.getFollowsSnapshot()) {
                    ps.setString(1, follow.getFollower().getUserId());
                    ps.setString(2, follow.getFollowee().getUserId());
                    setTimestamp(ps, 3, follow.getCreatedAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO blocks (blocker_id, blocked_id, created_at) VALUES (?,?,?)")) {
                for (Block block : model.getBlocksSnapshot()) {
                    ps.setString(1, block.getBlocker().getUserId());
                    ps.setString(2, block.getBlocked().getUserId());
                    setTimestamp(ps, 3, block.getCreatedAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO achievements (achievement_id, user_id, title, period, date_earned)
                    VALUES (?,?,?,?,?)
                    """)) {
                for (User user : model.getUsersSnapshot()) {
                    for (Achievement achievement : user.getAchievements()) {
                        ps.setString(1, achievement.getAchievementId());
                        ps.setString(2, user.getUserId());
                        ps.setString(3, achievement.getTitle());
                        ps.setString(4, achievement.getLeaderboardPeriod().name());
                        setTimestamp(ps, 5, achievement.getDateEarned());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO leaderboards (period, leaderboard_id, last_reset_time) VALUES (?,?,?)")) {
                for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                    Leaderboard board = model.getLeaderboard(period);
                    if (board == null) {
                        continue;
                    }
                    ps.setString(1, period.name());
                    ps.setString(2, board.getLeaderboardId());
                    setTimestamp(ps, 3, board.getLastResetTime());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException ex) {
            throw new StorageException("Failed to save VoteUp snapshot to H2", ex);
        }
    }

    /**
     * Clears domain collections on {@code model} and reloads from H2 (sessions untouched by caller).
     *
     * @param model target model
     * @throws StorageException on SQL failure
     */
    public void loadInto(VoteUpModel model) throws StorageException {
        try (Connection conn = connect()) {
            Map<String, User> users = new HashMap<>();
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM users")) {
                while (rs.next()) {
                    User user = new User(
                            rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"));
                    user.setBio(rs.getString("bio"));
                    user.setProfilePictureUrl(rs.getString("profile_picture_url"));
                    user.setRole(Role.valueOf(rs.getString("role")));
                    user.setDeleted(rs.getBoolean("is_deleted"));
                    user.setPermanentlyBanned(rs.getBoolean("is_permanently_banned"));
                    user.setBanExpiresAt(getTimestamp(rs, "ban_expires_at"));
                    user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
                    user.setLockoutExpiresAt(getTimestamp(rs, "lockout_expires_at"));
                    user.setLastFailedLogin(getTimestamp(rs, "last_failed_login"));
                    user.setLockoutTier(rs.getInt("lockout_tier"));
                    user.setLastLockEndedAt(getTimestamp(rs, "last_lock_ended_at"));
                    user.setCreatedAt(getTimestamp(rs, "created_at"));
                    users.put(user.getUserId(), user);
                }
            }

            Map<String, Post> posts = new HashMap<>();
            List<Object[]> parentLinks = new ArrayList<>();
            List<Object[]> deletedByLinks = new ArrayList<>();
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM posts")) {
                while (rs.next()) {
                    String postId = rs.getString("post_id");
                    User author = users.get(rs.getString("author_id"));
                    Post post = new Post(
                            postId,
                            author,
                            rs.getString("text_content"),
                            rs.getString("image_url"));
                    post.setCreatedAt(getTimestamp(rs, "created_at"));
                    post.setDeleted(rs.getBoolean("is_deleted"));
                    post.setLikeCount(rs.getInt("like_count"));
                    post.setDislikeCount(rs.getInt("dislike_count"));
                    String parentId = rs.getString("parent_post_id");
                    if (parentId != null) {
                        parentLinks.add(new Object[] {post, parentId});
                    }
                    String deletedById = rs.getString("deleted_by_id");
                    if (deletedById != null) {
                        deletedByLinks.add(new Object[] {post, deletedById});
                    }
                    posts.put(postId, post);
                }
            }
            for (Object[] link : parentLinks) {
                Post post = (Post) link[0];
                Post parent = posts.get((String) link[1]);
                // Reconstruct reply via reflection-free approach: new Post with parent
                // Existing instance has null parent — replace in map with parented copy.
                Post withParent = new Post(
                        post.getPostId(),
                        post.getAuthor(),
                        post.getTextContent(),
                        post.getImageUrl(),
                        parent);
                withParent.setCreatedAt(post.getCreatedAt());
                withParent.setDeleted(post.isDeleted());
                withParent.setLikeCount(post.getLikeCount());
                withParent.setDislikeCount(post.getDislikeCount());
                posts.put(withParent.getPostId(), withParent);
            }
            for (Object[] link : deletedByLinks) {
                String postId = ((Post) link[0]).getPostId();
                Post post = posts.get(postId);
                post.setDeletedBy(users.get((String) link[1]));
            }

            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT post_id, tag_name FROM post_tags")) {
                while (rs.next()) {
                    Post post = posts.get(rs.getString("post_id"));
                    if (post != null) {
                        post.addTag(new Tag(rs.getString("tag_name")));
                    }
                }
            }

            List<Engagement> engagements = new ArrayList<>();
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM engagements")) {
                while (rs.next()) {
                    Engagement engagement = new Engagement(
                            rs.getString("engagement_id"),
                            users.get(rs.getString("user_id")),
                            posts.get(rs.getString("post_id")),
                            EngagementType.valueOf(rs.getString("type")));
                    engagement.setCreatedAt(getTimestamp(rs, "created_at"));
                    engagements.add(engagement);
                }
            }

            List<Follow> follows = new ArrayList<>();
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM follows")) {
                while (rs.next()) {
                    Follow follow = new Follow(
                            users.get(rs.getString("follower_id")),
                            users.get(rs.getString("followee_id")));
                    follow.setCreatedAt(getTimestamp(rs, "created_at"));
                    follows.add(follow);
                }
            }

            List<Block> blocks = new ArrayList<>();
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM blocks")) {
                while (rs.next()) {
                    Block block = new Block(
                            users.get(rs.getString("blocker_id")),
                            users.get(rs.getString("blocked_id")));
                    block.setCreatedAt(getTimestamp(rs, "created_at"));
                    blocks.add(block);
                }
            }

            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM achievements")) {
                while (rs.next()) {
                    User user = users.get(rs.getString("user_id"));
                    if (user == null) {
                        continue;
                    }
                    Achievement achievement = new Achievement(
                            rs.getString("achievement_id"),
                            user,
                            rs.getString("title"),
                            LeaderboardPeriod.valueOf(rs.getString("period")));
                    achievement.setDateEarned(getTimestamp(rs, "date_earned"));
                    user.getAchievements().add(achievement);
                }
            }

            Map<LeaderboardPeriod, LocalDateTime> resets = new HashMap<>();
            Map<LeaderboardPeriod, String> ids = new HashMap<>();
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM leaderboards")) {
                while (rs.next()) {
                    LeaderboardPeriod period = LeaderboardPeriod.valueOf(rs.getString("period"));
                    ids.put(period, rs.getString("leaderboard_id"));
                    resets.put(period, getTimestamp(rs, "last_reset_time"));
                }
            }

            model.replaceDomainState(users, posts, engagements, follows, blocks, ids, resets);
        } catch (SQLException ex) {
            throw new StorageException("Failed to load VoteUp snapshot from H2", ex);
        }
    }

    private static void setTimestamp(PreparedStatement ps, int index, LocalDateTime value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static LocalDateTime getTimestamp(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }
}
