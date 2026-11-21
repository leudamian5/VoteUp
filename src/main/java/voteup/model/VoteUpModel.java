package voteup.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import voteup.model.exception.AuthenticationException;
import voteup.model.exception.AuthorizationException;
import voteup.model.exception.BannedException;
import voteup.model.exception.EngagementException;
import voteup.model.exception.LockoutException;
import voteup.model.exception.ModerationException;
import voteup.model.exception.PostEmptyException;
import voteup.model.exception.PostForbiddenException;
import voteup.model.exception.PostNotFoundException;
import voteup.model.exception.ProfileUnavailableException;
import voteup.model.exception.ReplyBlockedException;
import voteup.model.exception.SocialException;
import voteup.model.exception.UserNotFoundException;
import voteup.model.exception.UsernameTakenException;
import voteup.model.exception.StorageException;
import voteup.model.exception.ValidationException;
import voteup.persistence.H2VoteUpStore;
import voteup.util.PasswordHasher;

/**
 * Application model facade for VoteUp. Holds domain state and exposes the
 * business API so the model package can be unit-tested without controller/view.
 */
public class VoteUpModel extends AbstractModel {

    /** Failed logins required before a lockout is applied. */
    public static final int FAILED_ATTEMPTS_BEFORE_LOCK = 5;

    /** First lockout duration in minutes (tier 0). */
    public static final int LOCKOUT_MINUTES_TIER0 = 15;

    /** Escalated lockout duration in minutes (tier 1). */
    public static final int LOCKOUT_MINUTES_TIER1 = 30;

    /** Hours after a lock ends before progression resets to tier 0. */
    public static final int LOCKOUT_PROGRESSION_RESET_HOURS = 24;

    /** Default session lifetime in hours. */
    public static final int SESSION_HOURS = 24;

    /** Maximum feed / search page size. */
    public static final int FEED_PAGE_LIMIT = 25;

    private final Map<String, User> users;
    private final Map<String, Post> posts;
    private final List<Engagement> engagements;
    private final List<Follow> follows;
    private final List<Block> blocks;
    private final Map<String, SessionToken> sessions;
    private final Map<LeaderboardPeriod, Leaderboard> leaderboards;

    /** Optional H2 snapshot store; {@code null} for pure in-memory (unit tests). */
    private final H2VoteUpStore store;

    /** Clock for auth/lockout timing; overridable in tests. */
    private Supplier<LocalDateTime> clock = LocalDateTime::now;

    /**
     * Creates an empty in-memory model with three leaderboard periods.
     */
    public VoteUpModel() {
        this(null);
    }

    /**
     * Creates a model optionally backed by an H2 file store.
     *
     * @param store H2 store, or {@code null} for in-memory only
     */
    public VoteUpModel(H2VoteUpStore store) {
        this.users = new HashMap<>();
        this.posts = new HashMap<>();
        this.engagements = new ArrayList<>();
        this.follows = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.sessions = new HashMap<>();
        this.leaderboards = new HashMap<>();
        this.leaderboards.put(LeaderboardPeriod.DAILY,
                new Leaderboard("LB_DAILY", LeaderboardPeriod.DAILY));
        this.leaderboards.put(LeaderboardPeriod.WEEKLY,
                new Leaderboard("LB_WEEKLY", LeaderboardPeriod.WEEKLY));
        this.leaderboards.put(LeaderboardPeriod.MONTHLY,
                new Leaderboard("LB_MONTHLY", LeaderboardPeriod.MONTHLY));
        this.store = store;
        if (store != null) {
            try {
                store.ensureSchema();
                if (!store.isEmpty()) {
                    store.loadInto(this);
                }
            } catch (StorageException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private void persist() {
        if (store == null) {
            return;
        }
        try {
            store.save(this);
        } catch (StorageException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Forces a snapshot write (e.g. after seed mutations that set fields directly).
     */
    public void flush() {
        persist();
    }

    /**
     * Reloads domain state from the H2 store. No-op when running in-memory only.
     * Process-local sessions are left in place; callers should re-bind user refs.
     */
    public void reloadFromStore() {
        if (store == null) {
            return;
        }
        try {
            store.loadInto(this);
            notifyChanged(new ModelEvent(this, 0, "STORE_RELOADED", null));
        } catch (StorageException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * @return {@code true} if this model is backed by H2
     */
    public boolean isPersistent() {
        return store != null;
    }

    /**
     * @param userId user id
     * @return user or {@code null}
     */
    public User findUserById(String userId) {
        if (userId == null) {
            return null;
        }
        return users.get(userId);
    }

    /** @return users snapshot for persistence */
    public List<User> getUsersSnapshot() {
        return new ArrayList<>(users.values());
    }

    /** @return posts snapshot for persistence */
    public List<Post> getPostsSnapshot() {
        return new ArrayList<>(posts.values());
    }

    /** @return engagements snapshot for persistence */
    public List<Engagement> getEngagementsSnapshot() {
        return new ArrayList<>(engagements);
    }

    /** @return follows snapshot for persistence */
    public List<Follow> getFollowsSnapshot() {
        return new ArrayList<>(follows);
    }

    /** @return blocks snapshot for persistence */
    public List<Block> getBlocksSnapshot() {
        return new ArrayList<>(blocks);
    }

    /**
     * Replaces in-memory domain collections after an H2 load. Sessions are not cleared.
     *
     * @param loadedUsers       users by id
     * @param loadedPosts       posts by id
     * @param loadedEngagements engagements
     * @param loadedFollows     follows
     * @param loadedBlocks      blocks
     * @param leaderboardIds    period → id (unused; constructor ids kept)
     * @param leaderboardResets period → last reset time
     */
    public void replaceDomainState(
            Map<String, User> loadedUsers,
            Map<String, Post> loadedPosts,
            List<Engagement> loadedEngagements,
            List<Follow> loadedFollows,
            List<Block> loadedBlocks,
            Map<LeaderboardPeriod, String> leaderboardIds,
            Map<LeaderboardPeriod, LocalDateTime> leaderboardResets) {
        users.clear();
        users.putAll(loadedUsers);
        posts.clear();
        posts.putAll(loadedPosts);
        engagements.clear();
        engagements.addAll(loadedEngagements);
        follows.clear();
        follows.addAll(loadedFollows);
        blocks.clear();
        blocks.addAll(loadedBlocks);
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            Leaderboard board = leaderboards.get(period);
            if (board == null) {
                continue;
            }
            board.getTopTenUsers().clear();
            LocalDateTime resetAt = leaderboardResets.get(period);
            if (resetAt != null) {
                board.setLastResetTime(resetAt);
            }
        }
    }

    /**
     * Overrides the clock used for login lockout and session timing (tests).
     *
     * @param clock time supplier; must not be {@code null}
     */
    void setClock(Supplier<LocalDateTime> clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    private LocalDateTime now() {
        return clock.get();
    }

    /**
     * Registers a new user account.
     *
     * @param username desired username
     * @param password plaintext password (stored hashed)
     * @return created user
     * @throws UsernameTakenException if an active user already has the name
     * @throws ValidationException    if input is invalid
     */
    public User register(String username, String password)
            throws UsernameTakenException, ValidationException {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password must not be blank");
        }
        if (findActiveUserByUsername(username) != null) {
            throw new UsernameTakenException("Username already taken: " + username);
        }

        String userId = UUID.randomUUID().toString();
        User user = new User(userId, username.trim(), PasswordHasher.hash(password));
        users.put(userId, user);
        notifyChanged(new ModelEvent(this, 0, "USER_REGISTERED", user));
        persist();
        return user;
    }

    /**
     * Authenticates a user and issues a session token.
     *
     * @param username username
     * @param password password
     * @return session token
     * @throws AuthenticationException if credentials are wrong
     * @throws LockoutException        if the account is lockout-blocked
     * @throws BannedException         if the account is banned
     */
    public SessionToken login(String username, String password)
            throws AuthenticationException, LockoutException, BannedException {
        LocalDateTime now = now();
        User user = findActiveUserByUsername(username);

        if (user == null) {
            throw new AuthenticationException("Invalid credentials");
        }

        maybeResetLockoutProgression(user, now);

        if (user.isPermanentlyBanned()) {
            throw new BannedException("Account is permanently banned", true, null);
        }
        if (user.isTempBanned(now)) {
            throw new BannedException("Account is temporarily banned", false, user.getBanExpiresAt());
        }
        if (user.isLockedOut(now)) {
            throw new LockoutException("Account is locked out", user.getLockoutExpiresAt());
        }

        if (!PasswordHasher.matches(password, user.getPasswordHash())) {
            recordFailedLogin(user, now);
            persist();
            throw new AuthenticationException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLockoutExpiresAt(null);
        user.setLastFailedLogin(null);

        SessionToken token = new SessionToken(
                UUID.randomUUID().toString(),
                user,
                now.plusHours(SESSION_HOURS));
        sessions.put(token.getTokenId(), token);
        notifyChanged(new ModelEvent(this, 0, "USER_LOGIN", user));
        persist();
        return token;
    }

    /**
     * Revokes sessions for the user.
     *
     * @param user user to log out
     * @throws AuthenticationException if the user/session is invalid
     */
    public void logout(User user) throws AuthenticationException {
        if (user == null || !users.containsKey(user.getUserId())) {
            throw new AuthenticationException("User is not authenticated");
        }
        revokeSessionsFor(user);
        notifyChanged(new ModelEvent(this, 0, "USER_LOGOUT", user));
    }

    private void revokeSessionsFor(User user) {
        for (SessionToken token : sessions.values()) {
            if (token.getUser().getUserId().equals(user.getUserId()) && !token.isRevoked()) {
                token.revoke();
            }
        }
    }

    private void requireStaff(User actor) throws AuthorizationException {
        if (actor == null || !users.containsKey(actor.getUserId()) || actor.isDeleted()) {
            throw new AuthorizationException("Not authorized");
        }
        if (actor.getRole() != Role.MOD && actor.getRole() != Role.ADMIN) {
            throw new AuthorizationException("Moderator or admin required");
        }
    }

    private void requireAdmin(User actor) throws AuthorizationException {
        if (actor == null || !users.containsKey(actor.getUserId()) || actor.isDeleted()) {
            throw new AuthorizationException("Not authorized");
        }
        if (actor.getRole() != Role.ADMIN) {
            throw new AuthorizationException("Admin required");
        }
    }

    private User requireModerationTarget(User target) throws ModerationException {
        if (target == null || target.getUserId() == null || !users.containsKey(target.getUserId())) {
            throw new ModerationException("Target user not found");
        }
        User stored = users.get(target.getUserId());
        if (stored.isDeleted()) {
            throw new ModerationException("Target user not found");
        }
        return stored;
    }

    private void assertCanBan(User actor, User target) throws ModerationException {
        if (actor.getUserId().equals(target.getUserId())) {
            throw new ModerationException("Cannot ban yourself");
        }
        if (target.getRole() == Role.ADMIN) {
            throw new ModerationException("Cannot ban an admin");
        }
        if (target.getRole() == Role.MOD && actor.getRole() != Role.ADMIN) {
            throw new ModerationException("Only an admin may ban a moderator");
        }
    }

    /**
     * Finds an active (non-deleted) user by username. Package-visible for tests.
     *
     * @param username username to find
     * @return user or {@code null}
     */
    User findActiveUserByUsername(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim();
        for (User user : users.values()) {
            if (!user.isDeleted() && user.getUsername().equals(trimmed)) {
                return user;
            }
        }
        return null;
    }

    private void maybeResetLockoutProgression(User user, LocalDateTime now) {
        LocalDateTime lastLockEndedAt = user.getLastLockEndedAt();
        if (lastLockEndedAt != null
                && !now.isBefore(lastLockEndedAt.plusHours(LOCKOUT_PROGRESSION_RESET_HOURS))) {
            user.setLockoutTier(0);
        }
    }

    private void recordFailedLogin(User user, LocalDateTime now) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        user.setLastFailedLogin(now);
        if (user.getFailedLoginAttempts() < FAILED_ATTEMPTS_BEFORE_LOCK) {
            return;
        }

        int minutes = user.getLockoutTier() == 0 ? LOCKOUT_MINUTES_TIER0 : LOCKOUT_MINUTES_TIER1;
        LocalDateTime unlockAt = now.plusMinutes(minutes);
        user.setLockoutExpiresAt(unlockAt);
        user.setLastLockEndedAt(unlockAt);
        if (user.getLockoutTier() == 0) {
            user.setLockoutTier(1);
        }
        user.setFailedLoginAttempts(0);
    }

    /**
     * Requires a known, non-deleted user (model-level "authenticated").
     *
     * @param user candidate user
     * @throws AuthenticationException if missing or deleted
     */
    private void requireAuthenticated(User user) throws AuthenticationException {
        if (user == null || !users.containsKey(user.getUserId()) || user.isDeleted()) {
            throw new AuthenticationException("User is not authenticated");
        }
    }

    /**
     * Rejects permanently or temporarily banned users.
     *
     * @param user candidate user
     * @throws BannedException if banned at {@link #now()}
     */
    private void requireNotBanned(User user) throws BannedException {
        LocalDateTime now = now();
        if (user.isPermanentlyBanned()) {
            throw new BannedException("Account is permanently banned", true, null);
        }
        if (user.isTempBanned(now)) {
            throw new BannedException("Account is temporarily banned", false, user.getBanExpiresAt());
        }
    }

    private static boolean hasContent(String text, String imageUrl) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasImage = imageUrl != null && !imageUrl.isBlank();
        return hasText || hasImage;
    }

    /**
     * Looks up a non-deleted post by id.
     *
     * @param post candidate post
     * @return stored post
     * @throws PostNotFoundException if null, missing, or soft-deleted
     */
    private Post requireActivePost(Post post) throws PostNotFoundException {
        if (post == null || post.getPostId() == null) {
            throw new PostNotFoundException("Post not found");
        }
        Post stored = posts.get(post.getPostId());
        if (stored == null || stored.isDeleted()) {
            throw new PostNotFoundException("Post not found");
        }
        return stored;
    }

    /**
     * Requires a known, non-deleted target user.
     *
     * @param target candidate user
     * @return stored user
     * @throws UserNotFoundException if missing or deleted
     */
    private User requireActiveTarget(User target) throws UserNotFoundException {
        if (target == null || target.getUserId() == null || !users.containsKey(target.getUserId())) {
            throw new UserNotFoundException("User not found");
        }
        User stored = users.get(target.getUserId());
        if (stored.isDeleted()) {
            throw new UserNotFoundException("User not found");
        }
        return stored;
    }

    /**
     * @param follower follower
     * @param followee followee
     * @return {@code true} if follower follows followee
     */
    public boolean isFollowing(User follower, User followee) {
        return findFollow(follower, followee) != null;
    }

    /**
     * @param blocker user who may have blocked
     * @param blocked potentially blocked user
     * @return {@code true} if a block exists
     */
    public boolean isBlocked(User blocker, User blocked) {
        if (blocker == null || blocked == null) {
            return false;
        }
        for (Block block : blocks) {
            if (block.getBlocker().getUserId().equals(blocker.getUserId())
                    && block.getBlocked().getUserId().equals(blocked.getUserId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param a one user
     * @param b other user
     * @return {@code true} if either user has blocked the other
     */
    boolean hasBlockBetween(User a, User b) {
        return isBlocked(a, b) || isBlocked(b, a);
    }

    /**
     * Visibility helper for feeds/profiles: no block in either direction.
     *
     * @param viewer viewer
     * @param author content author / profile owner
     * @return {@code true} if viewer may see author's content
     */
    boolean canViewContent(User viewer, User author) {
        if (viewer == null || author == null) {
            return false;
        }
        return !hasBlockBetween(viewer, author);
    }

    /**
     * Seeds a block relationship without unfollow side effects (tests).
     *
     * @param blocker blocker
     * @param blocked blocked user
     */
    void putBlock(User blocker, User blocked) {
        blocks.add(new Block(blocker, blocked));
    }

    private Follow findFollow(User follower, User followee) {
        if (follower == null || followee == null) {
            return null;
        }
        for (Follow follow : follows) {
            if (follow.getFollower().getUserId().equals(follower.getUserId())
                    && follow.getFollowee().getUserId().equals(followee.getUserId())) {
                return follow;
            }
        }
        return null;
    }

    private void removeFollowsBothWays(User a, User b) {
        follows.removeIf(f ->
                (f.getFollower().getUserId().equals(a.getUserId())
                        && f.getFollowee().getUserId().equals(b.getUserId()))
                        || (f.getFollower().getUserId().equals(b.getUserId())
                        && f.getFollowee().getUserId().equals(a.getUserId())));
    }

    private Block findBlock(User blocker, User blocked) {
        if (blocker == null || blocked == null) {
            return null;
        }
        for (Block block : blocks) {
            if (block.getBlocker().getUserId().equals(blocker.getUserId())
                    && block.getBlocked().getUserId().equals(blocked.getUserId())) {
                return block;
            }
        }
        return null;
    }

    /**
     * Finds the engagement for a user on a post, if any.
     *
     * @param user user
     * @param post post
     * @return engagement or {@code null}
     */
    Engagement findEngagement(User user, Post post) {
        if (user == null || post == null) {
            return null;
        }
        for (Engagement engagement : engagements) {
            if (engagement.getUser().getUserId().equals(user.getUserId())
                    && engagement.getPost().getPostId().equals(post.getPostId())) {
                return engagement;
            }
        }
        return null;
    }

    /**
     * @param post post
     * @return number of engagement rows for the post
     */
    int countEngagementsFor(Post post) {
        if (post == null) {
            return 0;
        }
        int count = 0;
        for (Engagement engagement : engagements) {
            if (engagement.getPost().getPostId().equals(post.getPostId())) {
                count++;
            }
        }
        return count;
    }

    private static void applyCountDelta(Post post, EngagementType from, EngagementType to) {
        if (from == EngagementType.LIKE) {
            post.adjustEngagementCounts(-1, 0);
        } else if (from == EngagementType.DISLIKE) {
            post.adjustEngagementCounts(0, -1);
        }
        if (to == EngagementType.LIKE) {
            post.adjustEngagementCounts(1, 0);
        } else if (to == EngagementType.DISLIKE) {
            post.adjustEngagementCounts(0, 1);
        }
    }

    /**
     * Soft-deletes the account and renames username to {@code deleted_<id>}.
     *
     * @param user account to delete (must be the authenticated user)
     * @throws AuthenticationException if not authenticated
     * @throws AuthorizationException  if not allowed
     */
    public void deleteAccount(User user)
            throws AuthenticationException, AuthorizationException {
        requireAuthenticated(user);
        User stored = users.get(user.getUserId());
        if (stored.isDeleted()) {
            throw new AuthenticationException("User is not authenticated");
        }
        stored.setDeleted(true);
        stored.setUsername("deleted_" + stored.getUserId());
        revokeSessionsFor(stored);
        notifyChanged(new ModelEvent(this, 0, "USER_DELETED", stored));
        persist();
    }

    /**
     * Updates bio and/or profile picture for the actor.
     *
     * @param actor              authenticated user
     * @param bio                new bio (may be null)
     * @param profilePictureUrl  new avatar (may be null to clear)
     * @throws AuthenticationException if not authenticated
     * @throws AuthorizationException  if actor cannot update
     */
    public void updateProfile(User actor, String bio, String profilePictureUrl)
            throws AuthenticationException, AuthorizationException {
        requireAuthenticated(actor);
        User stored = users.get(actor.getUserId());
        stored.setBio(bio);
        stored.setProfilePictureUrl(profilePictureUrl);
        notifyChanged(new ModelEvent(this, 0, "PROFILE_UPDATED", stored));
        persist();
    }

    /**
     * Returns a profile visible to the viewer.
     *
     * @param viewer viewer
     * @param target profile owner
     * @return target user (or deleted stub representation)
     * @throws ProfileUnavailableException if blocked from viewing
     * @throws UserNotFoundException       if target does not exist
     */
    public User getProfile(User viewer, User target)
            throws ProfileUnavailableException, UserNotFoundException {
        if (target == null || target.getUserId() == null || !users.containsKey(target.getUserId())) {
            throw new UserNotFoundException("User not found");
        }
        User stored = users.get(target.getUserId());
        if (viewer != null
                && !viewer.getUserId().equals(stored.getUserId())
                && hasBlockBetween(viewer, stored)) {
            throw new ProfileUnavailableException("Profile unavailable due to a block");
        }
        return stored;
    }

    /**
     * @param actor  follower
     * @param target followee
     * @throws AuthenticationException if not authenticated
     * @throws SocialException         if follow not allowed
     * @throws UserNotFoundException   if target missing
     */
    public void followUser(User actor, User target)
            throws AuthenticationException, SocialException, UserNotFoundException {
        requireAuthenticated(actor);
        User followee = requireActiveTarget(target);
        if (actor.getUserId().equals(followee.getUserId())) {
            throw new SocialException("Cannot follow yourself");
        }
        if (hasBlockBetween(actor, followee)) {
            throw new SocialException("Cannot follow while a block exists");
        }
        if (isFollowing(actor, followee)) {
            throw new SocialException("Already following this user");
        }
        follows.add(new Follow(actor, followee));
        notifyChanged(new ModelEvent(this, 0, "USER_FOLLOWED", followee));
        persist();
    }

    /**
     * @param actor  actor
     * @param target former followee
     * @throws AuthenticationException if not authenticated
     * @throws SocialException         if unfollow not allowed
     * @throws UserNotFoundException   if target missing
     */
    public void unfollowUser(User actor, User target)
            throws AuthenticationException, SocialException, UserNotFoundException {
        requireAuthenticated(actor);
        User followee = requireActiveTarget(target);
        Follow existing = findFollow(actor, followee);
        if (existing == null) {
            throw new SocialException("Not following this user");
        }
        follows.remove(existing);
        notifyChanged(new ModelEvent(this, 0, "USER_UNFOLLOWED", followee));
        persist();
    }

    /**
     * Blocks {@code target} and removes follows both ways.
     *
     * @param actor  blocker
     * @param target blocked user
     * @throws AuthenticationException if not authenticated
     * @throws SocialException         if block not allowed
     * @throws UserNotFoundException   if target missing
     */
    public void blockUser(User actor, User target)
            throws AuthenticationException, SocialException, UserNotFoundException {
        requireAuthenticated(actor);
        User blocked = requireActiveTarget(target);
        if (actor.getUserId().equals(blocked.getUserId())) {
            throw new SocialException("Cannot block yourself");
        }
        if (isBlocked(actor, blocked)) {
            throw new SocialException("User is already blocked");
        }
        removeFollowsBothWays(actor, blocked);
        blocks.add(new Block(actor, blocked));
        notifyChanged(new ModelEvent(this, 0, "USER_BLOCKED", blocked));
        persist();
    }

    /**
     * @param actor  blocker
     * @param target previously blocked user
     * @throws AuthenticationException if not authenticated
     * @throws SocialException         if unblock not allowed
     * @throws UserNotFoundException   if target missing
     */
    public void unblockUser(User actor, User target)
            throws AuthenticationException, SocialException, UserNotFoundException {
        requireAuthenticated(actor);
        User blocked = requireActiveTarget(target);
        Block existing = findBlock(actor, blocked);
        if (existing == null) {
            throw new SocialException("User is not blocked");
        }
        blocks.remove(existing);
        notifyChanged(new ModelEvent(this, 0, "USER_UNBLOCKED", blocked));
        persist();
    }

    /**
     * Creates a top-level post.
     *
     * @param author   author
     * @param text     text content
     * @param imageUrl image path/URL
     * @param tags     initial tags
     * @return created post
     * @throws AuthenticationException if not authenticated
     * @throws PostEmptyException      if no text and no image
     * @throws BannedException         if author is banned
     */
    public Post createPost(User author, String text, String imageUrl, List<Tag> tags)
            throws AuthenticationException, PostEmptyException, BannedException {
        requireAuthenticated(author);
        requireNotBanned(author);
        if (!hasContent(text, imageUrl)) {
            throw new PostEmptyException("Post must have text and/or image");
        }

        Post post = new Post(UUID.randomUUID().toString(), author, text, imageUrl);
        post.setCreatedAt(now());
        if (tags != null) {
            for (Tag tag : tags) {
                post.addTag(tag);
            }
        }
        posts.put(post.getPostId(), post);
        notifyChanged(new ModelEvent(this, 0, "POST_CREATED", post));
        persist();
        return post;
    }

    /**
     * Creates a reply (full post with parent). Content may be text and/or image.
     *
     * @param author   author
     * @param parent   parent post
     * @param text     text
     * @param imageUrl image
     * @return reply post
     * @throws AuthenticationException if not authenticated
     * @throws PostEmptyException      if empty content
     * @throws ReplyBlockedException   if blocked by parent author
     * @throws PostNotFoundException   if parent missing/deleted
     * @throws BannedException         if author is banned
     */
    public Post createReply(User author, Post parent, String text, String imageUrl)
            throws AuthenticationException, PostEmptyException, ReplyBlockedException,
            PostNotFoundException, BannedException {
        requireAuthenticated(author);
        requireNotBanned(author);
        Post storedParent = requireActivePost(parent);
        if (!hasContent(text, imageUrl)) {
            throw new PostEmptyException("Reply must have text and/or image");
        }
        if (isBlocked(storedParent.getAuthor(), author)) {
            throw new ReplyBlockedException("Blocked by parent author");
        }

        Post reply = new Post(UUID.randomUUID().toString(), author, text, imageUrl, storedParent);
        reply.setCreatedAt(now());
        posts.put(reply.getPostId(), reply);
        notifyChanged(new ModelEvent(this, 0, "REPLY_CREATED", reply));
        persist();
        return reply;
    }

    /**
     * Soft-deletes a post (author, MOD, or ADMIN).
     *
     * @param actor actor
     * @param post  post
     * @throws AuthenticationException if not authenticated
     * @throws PostForbiddenException  if not allowed
     * @throws PostNotFoundException   if missing
     */
    public void deletePost(User actor, Post post)
            throws AuthenticationException, PostForbiddenException, PostNotFoundException {
        requireAuthenticated(actor);
        Post stored = requireActivePost(post);
        boolean isAuthor = stored.getAuthor().getUserId().equals(actor.getUserId());
        boolean isStaff = actor.getRole() == Role.MOD || actor.getRole() == Role.ADMIN;
        if (!isAuthor && !isStaff) {
            throw new PostForbiddenException("Not allowed to delete this post");
        }
        stored.setDeleted(true);
        stored.setDeletedBy(actor);
        notifyChanged(new ModelEvent(this, 0, "POST_DELETED", stored));
        persist();
    }

    /**
     * @param actor actor (must be author)
     * @param post  post
     * @param tags  tags to add
     * @throws AuthenticationException if not authenticated
     * @throws PostForbiddenException  if not author
     * @throws PostNotFoundException   if missing/deleted
     */
    public void addTags(User actor, Post post, List<Tag> tags)
            throws AuthenticationException, PostForbiddenException, PostNotFoundException {
        requireAuthenticated(actor);
        Post stored = requireActivePost(post);
        if (!stored.getAuthor().getUserId().equals(actor.getUserId())) {
            throw new PostForbiddenException("Only the author may add tags");
        }
        if (tags != null) {
            for (Tag tag : tags) {
                stored.addTag(tag);
            }
        }
        notifyChanged(new ModelEvent(this, 0, "POST_TAGS_ADDED", stored));
        persist();
    }

    /**
     * @param actor actor (must be author)
     * @param post  post
     * @param tags  tags to remove
     * @throws AuthenticationException if not authenticated
     * @throws PostForbiddenException  if not author
     * @throws PostNotFoundException   if missing/deleted
     */
    public void removeTags(User actor, Post post, List<Tag> tags)
            throws AuthenticationException, PostForbiddenException, PostNotFoundException {
        requireAuthenticated(actor);
        Post stored = requireActivePost(post);
        if (!stored.getAuthor().getUserId().equals(actor.getUserId())) {
            throw new PostForbiddenException("Only the author may remove tags");
        }
        if (tags != null) {
            for (Tag tag : tags) {
                stored.removeTag(tag);
            }
        }
        notifyChanged(new ModelEvent(this, 0, "POST_TAGS_REMOVED", stored));
        persist();
    }

    /**
     * Applies LIKE or DISLIKE (upsert; switches reset createdAt).
     *
     * @param user engagers
     * @param post target
     * @param type LIKE or DISLIKE
     * @throws AuthenticationException if not authenticated
     * @throws EngagementException     if engagement invalid
     * @throws PostNotFoundException   if post missing/deleted
     */
    public void applyEngagement(User user, Post post, EngagementType type)
            throws AuthenticationException, EngagementException, PostNotFoundException {
        requireAuthenticated(user);
        Post stored = requireActivePost(post);
        if (type == null) {
            throw new EngagementException("Engagement type is required");
        }

        Engagement existing = findEngagement(user, stored);
        if (existing == null) {
            Engagement created = new Engagement(
                    UUID.randomUUID().toString(), user, stored, type);
            created.setCreatedAt(now());
            engagements.add(created);
            applyCountDelta(stored, null, type);
            notifyChanged(new ModelEvent(this, 0, "ENGAGEMENT_APPLIED", created));
            persist();
            return;
        }

        if (existing.getType() == type) {
            return;
        }

        applyCountDelta(stored, existing.getType(), type);
        existing.setType(type);
        existing.setCreatedAt(now());
        notifyChanged(new ModelEvent(this, 0, "ENGAGEMENT_SWITCHED", existing));
        persist();
    }

    /**
     * Removes the user's engagement on a post if present.
     *
     * @param user user
     * @param post post
     * @throws AuthenticationException if not authenticated
     * @throws EngagementException     if removal fails
     * @throws PostNotFoundException   if post missing
     */
    public void removeEngagement(User user, Post post)
            throws AuthenticationException, EngagementException, PostNotFoundException {
        requireAuthenticated(user);
        if (post == null || post.getPostId() == null || !posts.containsKey(post.getPostId())) {
            throw new PostNotFoundException("Post not found");
        }
        Post stored = posts.get(post.getPostId());
        Engagement existing = findEngagement(user, stored);
        if (existing == null) {
            throw new EngagementException("No engagement to remove");
        }
        engagements.remove(existing);
        applyCountDelta(stored, existing.getType(), null);
        notifyChanged(new ModelEvent(this, 0, "ENGAGEMENT_REMOVED", stored));
        persist();
    }

    /**
     * Main feed: top-level, visible, newest first, paginated.
     *
     * @param viewer viewer (may be null only if product allows; stub assumes present)
     * @param offset page offset
     * @param limit  page size (typically 25)
     * @return posts
     * @throws ValidationException if paging args invalid
     */
    public List<Post> getMainFeed(User viewer, int offset, int limit)
            throws ValidationException {
        validatePaging(offset, limit);
        List<Post> matches = new ArrayList<>();
        for (Post post : posts.values()) {
            if (isVisibleTopLevel(viewer, post)) {
                matches.add(post);
            }
        }
        matches.sort(feedOrder());
        return page(matches, offset, limit);
    }

    /**
     * Following feed for the authenticated viewer.
     *
     * @param viewer follower
     * @param offset offset
     * @param limit  limit
     * @return posts
     * @throws AuthenticationException if not authenticated
     * @throws ValidationException     if paging invalid
     */
    public List<Post> getFollowingFeed(User viewer, int offset, int limit)
            throws AuthenticationException, ValidationException {
        requireAuthenticated(viewer);
        validatePaging(offset, limit);
        List<Post> matches = new ArrayList<>();
        for (Post post : posts.values()) {
            if (!isVisibleTopLevel(viewer, post)) {
                continue;
            }
            if (isFollowing(viewer, post.getAuthor())) {
                matches.add(post);
            }
        }
        matches.sort(feedOrder());
        return page(matches, offset, limit);
    }

    /**
     * Tag-filtered feed.
     *
     * @param viewer viewer
     * @param tag    tag
     * @param offset offset
     * @param limit  limit
     * @return posts
     * @throws ValidationException if args invalid
     */
    public List<Post> getTagFeed(User viewer, Tag tag, int offset, int limit)
            throws ValidationException {
        validatePaging(offset, limit);
        if (tag == null || tag.getName() == null || tag.getName().isBlank()) {
            throw new ValidationException("Tag is required");
        }
        String wanted = Tag.normalize(tag.getName()).getName();
        List<Post> matches = new ArrayList<>();
        for (Post post : posts.values()) {
            if (!isVisibleTopLevel(viewer, post)) {
                continue;
            }
            if (postHasTag(post, wanted)) {
                matches.add(post);
            }
        }
        matches.sort(feedOrder());
        return page(matches, offset, limit);
    }

    /**
     * Returns a post and its non-deleted replies.
     *
     * @param viewer viewer
     * @param post   root post
     * @return root plus replies
     * @throws PostNotFoundException if unavailable to viewer
     */
    public List<Post> getPostWithReplies(User viewer, Post post)
            throws PostNotFoundException {
        Post root = requireActivePost(post);
        if (viewer != null && !canViewContent(viewer, root.getAuthor())) {
            throw new PostNotFoundException("Post not found");
        }
        List<Post> replies = new ArrayList<>();
        for (Post candidate : posts.values()) {
            if (candidate.isDeleted() || !candidate.isReply()) {
                continue;
            }
            if (!candidate.getParentPost().getPostId().equals(root.getPostId())) {
                continue;
            }
            if (viewer != null && !canViewContent(viewer, candidate.getAuthor())) {
                continue;
            }
            replies.add(candidate);
        }
        replies.sort(feedOrder());
        List<Post> result = new ArrayList<>(1 + replies.size());
        result.add(root);
        result.addAll(replies);
        return result;
    }

    /**
     * Searches active users by query string.
     *
     * @param viewer viewer
     * @param query  search text
     * @return matching users
     * @throws AuthenticationException if required and missing
     * @throws ValidationException     if query empty
     */
    public List<User> searchUsers(User viewer, String query)
            throws AuthenticationException, ValidationException {
        requireAuthenticated(viewer);
        if (query == null || query.isBlank()) {
            throw new ValidationException("Search query must not be blank");
        }
        String needle = query.trim().toLowerCase();
        List<User> matches = new ArrayList<>();
        for (User user : users.values()) {
            if (user.isDeleted()) {
                continue;
            }
            if (!canViewContent(viewer, user)) {
                continue;
            }
            if (user.getUsername().toLowerCase().contains(needle)) {
                matches.add(user);
            }
        }
        matches.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        return pageUsers(matches, 0, FEED_PAGE_LIMIT);
    }

    /**
     * Searches posts by tag name (leading {@code #} optional).
     *
     * @param viewer  viewer
     * @param tagName tag name
     * @return matching posts
     * @throws AuthenticationException if required and missing
     * @throws ValidationException     if query empty
     */
    public List<Post> searchByTag(User viewer, String tagName)
            throws AuthenticationException, ValidationException {
        requireAuthenticated(viewer);
        if (tagName == null || tagName.isBlank()) {
            throw new ValidationException("Tag query must not be blank");
        }
        return getTagFeed(viewer, Tag.normalize(tagName), 0, FEED_PAGE_LIMIT);
    }

    private void validatePaging(int offset, int limit) throws ValidationException {
        if (offset < 0) {
            throw new ValidationException("Offset must be non-negative");
        }
        if (limit < 1 || limit > FEED_PAGE_LIMIT) {
            throw new ValidationException(
                    "Limit must be between 1 and " + FEED_PAGE_LIMIT);
        }
    }

    private static Comparator<Post> feedOrder() {
        return Comparator
                .comparing(Post::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(p -> p.getAuthor().getCreatedAt())
                .thenComparing(Post::getPostId);
    }

    private boolean isVisibleTopLevel(User viewer, Post post) {
        if (post.isDeleted() || post.isReply()) {
            return false;
        }
        return viewer == null || canViewContent(viewer, post.getAuthor());
    }

    private static boolean postHasTag(Post post, String normalizedName) {
        for (Tag tag : post.getTags()) {
            if (tag.getName().equalsIgnoreCase(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    private static List<Post> page(List<Post> sorted, int offset, int limit) {
        if (offset >= sorted.size()) {
            return List.of();
        }
        int end = Math.min(offset + limit, sorted.size());
        return new ArrayList<>(sorted.subList(offset, end));
    }

    private static List<User> pageUsers(List<User> sorted, int offset, int limit) {
        if (offset >= sorted.size()) {
            return List.of();
        }
        int end = Math.min(offset + limit, sorted.size());
        return new ArrayList<>(sorted.subList(offset, end));
    }

    /**
     * Issues a temporary ban and revokes the target's sessions.
     *
     * @param mod           moderator or admin
     * @param target        user to ban
     * @param durationHours ban length in hours
     * @throws AuthorizationException if actor lacks privilege
     * @throws ModerationException    if target/action invalid
     * @throws ValidationException    if duration invalid
     */
    public void issueTempBan(User mod, User target, int durationHours)
            throws AuthorizationException, ModerationException, ValidationException {
        requireStaff(mod);
        if (durationHours < 1) {
            throw new ValidationException("Ban duration must be at least 1 hour");
        }
        User stored = requireModerationTarget(target);
        assertCanBan(mod, stored);
        if (stored.isPermanentlyBanned()) {
            throw new ModerationException("User is permanently banned");
        }
        stored.setBanExpiresAt(now().plusHours(durationHours));
        revokeSessionsFor(stored);
        notifyChanged(new ModelEvent(this, 0, "USER_TEMP_BANNED", stored));
        persist();
    }

    /**
     * Permanently bans a user (posts remain; username not renamed like delete).
     *
     * @param mod    moderator or admin
     * @param target user to ban
     * @throws AuthorizationException if actor lacks privilege
     * @throws ModerationException    if target/action invalid
     */
    public void issuePermBan(User mod, User target)
            throws AuthorizationException, ModerationException {
        requireStaff(mod);
        User stored = requireModerationTarget(target);
        assertCanBan(mod, stored);
        if (stored.isPermanentlyBanned()) {
            throw new ModerationException("User is already permanently banned");
        }
        stored.setPermanentlyBanned(true);
        stored.setBanExpiresAt(null);
        revokeSessionsFor(stored);
        notifyChanged(new ModelEvent(this, 0, "USER_PERM_BANNED", stored));
        persist();
    }

    /**
     * Promotes a USER to MOD (admin only).
     *
     * @param admin  admin
     * @param target user
     * @throws AuthorizationException if not admin
     * @throws ModerationException    if target invalid
     */
    public void promoteToMod(User admin, User target)
            throws AuthorizationException, ModerationException {
        requireAdmin(admin);
        User stored = requireModerationTarget(target);
        if (stored.getRole() != Role.USER) {
            throw new ModerationException("Only a USER can be promoted to MOD");
        }
        stored.setRole(Role.MOD);
        notifyChanged(new ModelEvent(this, 0, "USER_PROMOTED", stored));
        persist();
    }

    /**
     * Demotes a MOD to USER (admin only).
     *
     * @param admin  admin
     * @param target mod
     * @throws AuthorizationException if not admin
     * @throws ModerationException    if target invalid
     */
    public void demoteFromMod(User admin, User target)
            throws AuthorizationException, ModerationException {
        requireAdmin(admin);
        User stored = requireModerationTarget(target);
        if (stored.getRole() != Role.MOD) {
            throw new ModerationException("Only a MOD can be demoted to USER");
        }
        stored.setRole(Role.USER);
        notifyChanged(new ModelEvent(this, 0, "USER_DEMOTED", stored));
        persist();
    }

    /**
     * @param period leaderboard period
     * @return leaderboard row
     */
    public Leaderboard getLeaderboard(LeaderboardPeriod period) {
        return leaderboards.get(period);
    }

    /**
     * @param period leaderboard period
     * @return current top standings
     */
    public List<User> getStandings(LeaderboardPeriod period) {
        if (period == null || !leaderboards.containsKey(period)) {
            return List.of();
        }
        Leaderboard board = leaderboards.get(period);
        List<User> top = computeTopTen(board);
        board.getTopTenUsers().clear();
        board.getTopTenUsers().addAll(top);
        return new ArrayList<>(top);
    }

    /**
     * Period net score (likes − dislikes since last reset) for a user.
     *
     * @param user   author
     * @param period leaderboard period
     * @return score, or {@code 0} if none / invalid args
     */
    public int getPeriodScore(User user, LeaderboardPeriod period) {
        if (user == null || period == null || !leaderboards.containsKey(period)) {
            return 0;
        }
        return computePeriodScores(leaderboards.get(period))
                .getOrDefault(user.getUserId(), 0);
    }

    /**
     * Resets a leaderboard period and awards achievements.
     *
     * @param period period to reset
     */
    public void resetLeaderboard(LeaderboardPeriod period) {
        if (period == null || !leaderboards.containsKey(period)) {
            return;
        }
        Leaderboard board = leaderboards.get(period);
        List<User> top = getStandings(period);
        LocalDateTime earnedAt = now();
        for (User user : top) {
            Achievement achievement = new Achievement(
                    UUID.randomUUID().toString(),
                    user,
                    "Top 10 " + period.name(),
                    period);
            achievement.setDateEarned(earnedAt);
            user.getAchievements().add(achievement);
        }
        board.reset();
        board.setLastResetTime(earnedAt);
        notifyChanged(new ModelEvent(this, 0, "LEADERBOARD_RESET", period));
        persist();
    }

    private Map<String, Integer> computePeriodScores(Leaderboard board) {
        LocalDateTime resetAt = board.getLastResetTime();
        Map<String, Integer> finalScores = new HashMap<>();
        for (Engagement engagement : engagements) {
            if (engagement.getCreatedAt() == null || resetAt == null) {
                continue;
            }
            // Strictly after last reset so a reset at time T excludes period-T engagements.
            if (!engagement.getCreatedAt().isAfter(resetAt)) {
                continue;
            }
            String authorId = engagement.getPost().getAuthor().getUserId();
            int delta = engagement.getType() == EngagementType.LIKE ? 1 : -1;
            finalScores.merge(authorId, delta, Integer::sum);
        }
        return finalScores;
    }

    private List<User> computeTopTen(Leaderboard board) {
        Map<String, Integer> finalScores = computePeriodScores(board);
        LocalDateTime resetAt = board.getLastResetTime();
        List<Engagement> periodEngagements = new ArrayList<>();
        for (Engagement engagement : engagements) {
            if (engagement.getCreatedAt() == null || resetAt == null) {
                continue;
            }
            if (!engagement.getCreatedAt().isAfter(resetAt)) {
                continue;
            }
            periodEngagements.add(engagement);
        }

        periodEngagements.sort(Comparator
                .comparing(Engagement::getCreatedAt)
                .thenComparing(Engagement::getEngagementId));

        Map<String, Integer> running = new HashMap<>();
        Map<String, LocalDateTime> firstReachedAt = new HashMap<>();
        for (Engagement engagement : periodEngagements) {
            String authorId = engagement.getPost().getAuthor().getUserId();
            int delta = engagement.getType() == EngagementType.LIKE ? 1 : -1;
            int next = running.getOrDefault(authorId, 0) + delta;
            running.put(authorId, next);
            Integer finalScore = finalScores.get(authorId);
            if (finalScore != null
                    && next == finalScore
                    && !firstReachedAt.containsKey(authorId)) {
                firstReachedAt.put(authorId, engagement.getCreatedAt());
            }
        }

        // Eligible users with no period engagements count as implicit score 0
        // (so zeros rank above negatives).
        List<User> candidates = new ArrayList<>();
        for (User user : users.values()) {
            if (user.isDeleted() || user.isPermanentlyBanned()) {
                continue;
            }
            candidates.add(user);
        }

        candidates.sort((u1, u2) -> {
            int s1 = finalScores.getOrDefault(u1.getUserId(), 0);
            int s2 = finalScores.getOrDefault(u2.getUserId(), 0);
            if (s1 != s2) {
                return Integer.compare(s2, s1);
            }
            User winner = board.resolveTie(
                    u1,
                    u2,
                    firstReachedAt.get(u1.getUserId()),
                    firstReachedAt.get(u2.getUserId()));
            if (winner == u1) {
                return -1;
            }
            if (winner == u2) {
                return 1;
            }
            return 0;
        });

        if (candidates.size() <= Leaderboard.TOP_TEN_SIZE) {
            return candidates;
        }
        return new ArrayList<>(candidates.subList(0, Leaderboard.TOP_TEN_SIZE));
    }
}
