package voteup.controller;

import java.util.List;

import javax.swing.SwingUtilities;

import voteup.model.EngagementType;
import voteup.model.LeaderboardPeriod;
import voteup.model.Post;
import voteup.model.Role;
import java.time.LocalDateTime;

import voteup.model.SessionToken;
import voteup.model.Tag;
import voteup.model.User;
import voteup.model.VoteUpModel;
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
import voteup.model.exception.ValidationException;
import voteup.view.FollowingFeedView;
import voteup.view.JFrameView;
import voteup.view.LoginView;
import voteup.view.MainFeedView;
import voteup.view.PostPageView;
import voteup.view.ProfileView;
import voteup.view.TagFeedView;

/**
 * Application controller. Holds session state, navigates Swing views, and
 * delegates business operations to {@link VoteUpModel}.
 */
public class VoteUpController extends AbstractController {

    private VoteUpModel voteUpModel;
    private SessionToken currentSession;

    private LoginView loginView;
    private MainFeedView mainFeedView;
    private FollowingFeedView followingFeedView;
    private TagFeedView tagFeedView;
    private PostPageView postPageView;
    private ProfileView profileView;

    /**
     * Creates a controller with a new {@link VoteUpModel}.
     */
    public VoteUpController() {
        this(new VoteUpModel());
    }

    /**
     * Creates a controller bound to an existing model.
     *
     * @param model application model
     */
    public VoteUpController(VoteUpModel model) {
        this.voteUpModel = model;
        setModel(model);
    }

    /**
     * @return VoteUp model
     */
    public VoteUpModel getVoteUpModel() {
        return voteUpModel;
    }

    /**
     * @return current session user, or {@code null}
     */
    public User getCurrentUser() {
        return currentSession == null ? null : currentSession.getUser();
    }

    /**
     * @return current session token, or {@code null}
     */
    public SessionToken getCurrentSession() {
        return currentSession;
    }

    /**
     * Reloads shared H2 state into the model and re-binds the current session user.
     * No-op when the model is in-memory only.
     */
    public void reloadFromStore() {
        String userId = getCurrentUser() == null ? null : getCurrentUser().getUserId();
        boolean wasRevoked = currentSession != null && currentSession.isRevoked();
        LocalDateTime expiresAt = currentSession == null ? null : currentSession.getExpiresAt();
        String tokenId = currentSession == null ? null : currentSession.getTokenId();

        voteUpModel.reloadFromStore();

        if (userId == null) {
            return;
        }
        User fresh = voteUpModel.findUserById(userId);
        if (fresh == null || fresh.isDeleted()) {
            currentSession = null;
            showLogin();
            return;
        }
        SessionToken rebound = new SessionToken(
                tokenId == null ? java.util.UUID.randomUUID().toString() : tokenId,
                fresh,
                expiresAt == null ? fresh.getCreatedAt().plusHours(VoteUpModel.SESSION_HOURS) : expiresAt);
        if (wasRevoked) {
            rebound.revoke();
        }
        currentSession = rebound;
    }

    /**
     * Builds views (if needed) and shows the login screen on the EDT.
     */
    public void start() {
        SwingUtilities.invokeLater(() -> {
            ensureViews();
            showLogin();
        });
    }

    private void ensureViews() {
        if (loginView == null) {
            loginView = new LoginView(voteUpModel, this);
            mainFeedView = new MainFeedView(voteUpModel, this);
            followingFeedView = new FollowingFeedView(voteUpModel, this);
            tagFeedView = new TagFeedView(voteUpModel, this);
            postPageView = new PostPageView(voteUpModel, this);
            profileView = new ProfileView(voteUpModel, this);
            setView(loginView);
        }
    }

    private void showOnly(JFrameView target) {
        ensureViews();
        JFrameView[] all = {
                loginView, mainFeedView, followingFeedView, tagFeedView, postPageView, profileView
        };
        for (JFrameView view : all) {
            if (view != null && view != target) {
                view.setVisible(false);
            }
        }
        setView(target);
        target.showWindow();
    }

    /** Shows the login / register screen. */
    public void showLogin() {
        showOnly(loginView);
    }

    /** Shows the main feed for the logged-in user. */
    public void showMainFeed() {
        showOnly(mainFeedView);
        mainFeedView.refreshFeed();
    }

    /** Shows the following feed. */
    public void showFollowingFeed() {
        showOnly(followingFeedView);
        followingFeedView.refreshFeed();
    }

    /**
     * Shows the tag feed for a tag name.
     *
     * @param tagName raw tag (leading {@code #} optional)
     */
    public void showTagFeed(String tagName) {
        showOnly(tagFeedView);
        tagFeedView.showTag(Tag.normalize(tagName));
    }

    /**
     * Shows a post thread.
     *
     * @param post root post
     */
    public void showPostPage(Post post) {
        showOnly(postPageView);
        postPageView.showPost(post);
    }

    /**
     * Shows a user profile.
     *
     * @param target profile owner
     */
    public void showProfile(User target) {
        showOnly(profileView);
        profileView.showProfile(target);
    }

    /**
     * Registers a new account.
     *
     * @param username username
     * @param password password
     * @return {@code true} if registration succeeded
     * @throws UsernameTakenException if username taken
     * @throws ValidationException    if input invalid
     */
    public boolean register(String username, String password)
            throws UsernameTakenException, ValidationException {
        voteUpModel.register(username, password);
        return true;
    }

    /**
     * @param username username
     * @param password password
     * @return session token
     * @throws AuthenticationException on bad credentials
     * @throws LockoutException        when locked out
     * @throws BannedException         when banned
     */
    public SessionToken login(String username, String password)
            throws AuthenticationException, LockoutException, BannedException {
        currentSession = voteUpModel.login(username, password);
        showMainFeed();
        return currentSession;
    }

    /**
     * Logs out the current user and returns to login.
     *
     * @throws AuthenticationException if invalid
     */
    public void logout() throws AuthenticationException {
        User user = getCurrentUser();
        if (user != null) {
            voteUpModel.logout(user);
        }
        currentSession = null;
        showLogin();
    }

    /**
     * @param user user to log out
     * @throws AuthenticationException if invalid
     */
    public void logout(User user) throws AuthenticationException {
        voteUpModel.logout(user);
        if (getCurrentUser() != null && getCurrentUser().getUserId().equals(user.getUserId())) {
            currentSession = null;
            showLogin();
        }
    }

    /**
     * @param user account to delete
     * @throws AuthenticationException if not authenticated
     * @throws AuthorizationException  if not allowed
     */
    public void deleteAccount(User user)
            throws AuthenticationException, AuthorizationException {
        voteUpModel.deleteAccount(user);
        if (getCurrentUser() != null && getCurrentUser().getUserId().equals(user.getUserId())) {
            currentSession = null;
            showLogin();
        }
    }

    /**
     * @param actor             actor
     * @param bio               bio
     * @param profilePictureUrl avatar
     * @throws AuthenticationException if not authenticated
     * @throws AuthorizationException  if not allowed
     */
    public void updateProfile(User actor, String bio, String profilePictureUrl)
            throws AuthenticationException, AuthorizationException {
        voteUpModel.updateProfile(actor, bio, profilePictureUrl);
    }

    /**
     * @param viewer viewer
     * @param target target
     * @return profile user
     * @throws ProfileUnavailableException if blocked
     * @throws UserNotFoundException       if missing
     */
    public User getProfile(User viewer, User target)
            throws ProfileUnavailableException, UserNotFoundException {
        return voteUpModel.getProfile(viewer, target);
    }

    /**
     * @param actor  actor
     * @param target target
     * @throws AuthenticationException if not authenticated
     * @throws SocialException         if not allowed
     * @throws UserNotFoundException   if missing
     */
    public void follow(User actor, User target)
            throws AuthenticationException, SocialException, UserNotFoundException {
        voteUpModel.followUser(actor, target);
    }

    /**
     * @param actor  actor
     * @param target target
     * @throws AuthenticationException if not authenticated
     * @throws SocialException         if not allowed
     * @throws UserNotFoundException   if missing
     */
    public void unfollow(User actor, User target)
            throws AuthenticationException, SocialException, UserNotFoundException {
        voteUpModel.unfollowUser(actor, target);
    }

    /**
     * @param actor  actor
     * @param target target
     * @throws AuthenticationException if not authenticated
     * @throws SocialException         if not allowed
     * @throws UserNotFoundException   if missing
     */
    public void block(User actor, User target)
            throws AuthenticationException, SocialException, UserNotFoundException {
        voteUpModel.blockUser(actor, target);
    }

    /**
     * @param actor  actor
     * @param target target
     * @throws AuthenticationException if not authenticated
     * @throws SocialException         if not allowed
     * @throws UserNotFoundException   if missing
     */
    public void unblock(User actor, User target)
            throws AuthenticationException, SocialException, UserNotFoundException {
        voteUpModel.unblockUser(actor, target);
    }

    /**
     * @param follower follower
     * @param followee followee
     * @return whether following
     */
    public boolean isFollowing(User follower, User followee) {
        return voteUpModel.isFollowing(follower, followee);
    }

    /**
     * @param blocker blocker
     * @param blocked blocked
     * @return whether blocked
     */
    public boolean isBlocked(User blocker, User blocked) {
        return voteUpModel.isBlocked(blocker, blocked);
    }

    /**
     * @param author author
     * @param text   text
     * @param img    image
     * @param tags   tags
     * @return post
     * @throws AuthenticationException if not authenticated
     * @throws PostEmptyException      if empty
     * @throws BannedException         if banned
     */
    public Post createPost(User author, String text, String img, List<Tag> tags)
            throws AuthenticationException, PostEmptyException, BannedException {
        return voteUpModel.createPost(author, text, img, tags);
    }

    /**
     * @param author author
     * @param parent parent
     * @param text   text
     * @param img    image
     * @return reply
     * @throws AuthenticationException if not authenticated
     * @throws PostEmptyException      if empty
     * @throws ReplyBlockedException   if blocked
     * @throws PostNotFoundException   if parent missing
     * @throws BannedException         if banned
     */
    public Post createReply(User author, Post parent, String text, String img)
            throws AuthenticationException, PostEmptyException, ReplyBlockedException,
            PostNotFoundException, BannedException {
        return voteUpModel.createReply(author, parent, text, img);
    }

    /**
     * @param actor actor
     * @param post  post
     * @throws AuthenticationException if not authenticated
     * @throws PostForbiddenException  if forbidden
     * @throws PostNotFoundException   if missing
     */
    public void deletePost(User actor, Post post)
            throws AuthenticationException, PostForbiddenException, PostNotFoundException {
        voteUpModel.deletePost(actor, post);
    }

    /**
     * @param actor actor
     * @param post  post
     * @param tags  tags
     * @throws AuthenticationException if not authenticated
     * @throws PostForbiddenException  if forbidden
     * @throws PostNotFoundException   if missing
     */
    public void addTags(User actor, Post post, List<Tag> tags)
            throws AuthenticationException, PostForbiddenException, PostNotFoundException {
        voteUpModel.addTags(actor, post, tags);
    }

    /**
     * @param actor actor
     * @param post  post
     * @param tags  tags
     * @throws AuthenticationException if not authenticated
     * @throws PostForbiddenException  if forbidden
     * @throws PostNotFoundException   if missing
     */
    public void removeTags(User actor, Post post, List<Tag> tags)
            throws AuthenticationException, PostForbiddenException, PostNotFoundException {
        voteUpModel.removeTags(actor, post, tags);
    }

    /**
     * @param user user
     * @param post post
     * @param type type
     * @throws AuthenticationException if not authenticated
     * @throws EngagementException     if invalid
     * @throws PostNotFoundException   if missing
     */
    public void applyEngagement(User user, Post post, EngagementType type)
            throws AuthenticationException, EngagementException, PostNotFoundException {
        voteUpModel.applyEngagement(user, post, type);
    }

    /**
     * @param user user
     * @param post post
     * @throws AuthenticationException if not authenticated
     * @throws EngagementException     if invalid
     * @throws PostNotFoundException   if missing
     */
    public void removeEngagement(User user, Post post)
            throws AuthenticationException, EngagementException, PostNotFoundException {
        voteUpModel.removeEngagement(user, post);
    }

    /**
     * @param viewer viewer
     * @param offset offset
     * @param limit  limit
     * @return posts
     * @throws ValidationException if paging invalid
     */
    public List<Post> getMainFeed(User viewer, int offset, int limit)
            throws ValidationException {
        return voteUpModel.getMainFeed(viewer, offset, limit);
    }

    /**
     * @param viewer viewer
     * @param offset offset
     * @param limit  limit
     * @return posts
     * @throws AuthenticationException if not authenticated
     * @throws ValidationException     if paging invalid
     */
    public List<Post> getFollowingFeed(User viewer, int offset, int limit)
            throws AuthenticationException, ValidationException {
        return voteUpModel.getFollowingFeed(viewer, offset, limit);
    }

    /**
     * @param viewer viewer
     * @param tag    tag
     * @param offset offset
     * @param limit  limit
     * @return posts
     * @throws ValidationException if invalid
     */
    public List<Post> getTagFeed(User viewer, Tag tag, int offset, int limit)
            throws ValidationException {
        return voteUpModel.getTagFeed(viewer, tag, offset, limit);
    }

    /**
     * @param viewer viewer
     * @param post   post
     * @return post and replies
     * @throws PostNotFoundException if unavailable
     */
    public List<Post> getPostWithReplies(User viewer, Post post)
            throws PostNotFoundException {
        return voteUpModel.getPostWithReplies(viewer, post);
    }

    /**
     * @param viewer viewer
     * @param query  query
     * @return users
     * @throws AuthenticationException if required
     * @throws ValidationException     if empty
     */
    public List<User> searchUsers(User viewer, String query)
            throws AuthenticationException, ValidationException {
        return voteUpModel.searchUsers(viewer, query);
    }

    /**
     * @param viewer  viewer
     * @param tagName tag
     * @return posts
     * @throws AuthenticationException if required
     * @throws ValidationException     if empty
     */
    public List<Post> searchByTag(User viewer, String tagName)
            throws AuthenticationException, ValidationException {
        return voteUpModel.searchByTag(viewer, tagName);
    }

    /**
     * @param mod           mod
     * @param target        target
     * @param durationHours hours
     * @throws AuthorizationException if forbidden
     * @throws ModerationException    if invalid
     * @throws ValidationException    if duration invalid
     */
    public void issueTempBan(User mod, User target, int durationHours)
            throws AuthorizationException, ModerationException, ValidationException {
        voteUpModel.issueTempBan(mod, target, durationHours);
    }

    /**
     * @param mod    mod
     * @param target target
     * @throws AuthorizationException if forbidden
     * @throws ModerationException    if invalid
     */
    public void issuePermBan(User mod, User target)
            throws AuthorizationException, ModerationException {
        voteUpModel.issuePermBan(mod, target);
    }

    /**
     * @param admin  admin
     * @param target target
     * @throws AuthorizationException if forbidden
     * @throws ModerationException    if invalid
     */
    public void promoteToMod(User admin, User target)
            throws AuthorizationException, ModerationException {
        voteUpModel.promoteToMod(admin, target);
    }

    /**
     * @param admin  admin
     * @param target target
     * @throws AuthorizationException if forbidden
     * @throws ModerationException    if invalid
     */
    public void demoteFromMod(User admin, User target)
            throws AuthorizationException, ModerationException {
        voteUpModel.demoteFromMod(admin, target);
    }

    /**
     * @param period period
     * @return standings
     */
    public List<User> getStandings(LeaderboardPeriod period) {
        return voteUpModel.getStandings(period);
    }

    /**
     * @param user   user
     * @param period period
     * @return period net score
     */
    public int getPeriodScore(User user, LeaderboardPeriod period) {
        return voteUpModel.getPeriodScore(user, period);
    }

    /**
     * @param period period to reset
     */
    public void resetLeaderboard(LeaderboardPeriod period) {
        voteUpModel.resetLeaderboard(period);
    }

    /**
     * @param user user
     * @return {@code true} if staff
     */
    public boolean isStaff(User user) {
        return user != null && (user.getRole() == Role.MOD || user.getRole() == Role.ADMIN);
    }
}
