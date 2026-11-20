package voteup.view;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import voteup.controller.Controller;
import voteup.controller.VoteUpController;
import voteup.model.EngagementType;
import voteup.model.Model;
import voteup.model.ModelEvent;
import voteup.model.Post;
import voteup.model.Tag;
import voteup.model.User;
import voteup.model.VoteUpModel;

/**
 * VoteUp main feed screen.
 */
public class MainFeedView extends JFrameView {

    private final JTextArea feedArea = new JTextArea();
    private List<Post> currentPosts = List.of();

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public MainFeedView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp — Main feed");
        buildUi();
    }

    private VoteUpController ctrl() {
        return (VoteUpController) getController();
    }

    private void buildUi() {
        ViewSupport.styleFrame(this, 820, 640);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(ViewSupport.BG);
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(ViewSupport.titledHeader("Main feed"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        center.add(new NavBar(ctrl(), this, this::refreshFeed), BorderLayout.NORTH);
        center.add(ViewSupport.postListScroll(feedArea), BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton compose = new JButton("New post");
        JButton open = new JButton("Open #");
        JButton like = new JButton("Like #");
        JButton dislike = new JButton("Dislike #");
        JButton profile = new JButton("Author profile #");
        actions.add(compose);
        actions.add(open);
        actions.add(like);
        actions.add(dislike);
        actions.add(profile);
        center.add(actions, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        compose.addActionListener(e -> composePost());
        open.addActionListener(e -> withSelectedPost(ctrl()::showPostPage));
        like.addActionListener(e -> engage(EngagementType.LIKE));
        dislike.addActionListener(e -> engage(EngagementType.DISLIKE));
        profile.addActionListener(e -> withSelectedPost(p -> ctrl().showProfile(p.getAuthor())));
    }

    private void composePost() {
        User me = ctrl().getCurrentUser();
        if (me == null) {
            return;
        }
        JTextField text = new JTextField();
        JTextField image = new JTextField();
        JTextField tags = new JTextField();
        Object[] fields = {
                "Text:", text,
                "Image URL/path (optional):", image,
                "Tags (comma-separated, optional):", tags
        };
        int ok = JOptionPane.showConfirmDialog(this, fields, "New post", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            List<Tag> tagList = parseTags(tags.getText());
            ctrl().createPost(me, blankToNull(text.getText()), blankToNull(image.getText()), tagList);
            refreshFeed();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private static List<Tag> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Tag::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void engage(EngagementType type) {
        User me = ctrl().getCurrentUser();
        withSelectedPost(post -> {
            try {
                ctrl().applyEngagement(me, post, type);
                refreshFeed();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
    }

    private void withSelectedPost(java.util.function.Consumer<Post> action) {
        if (currentPosts.isEmpty()) {
            showError("No posts to select.");
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Post number (1-" + currentPosts.size() + "):", "1");
        if (input == null) {
            return;
        }
        try {
            int idx = Integer.parseInt(input.trim()) - 1;
            if (idx < 0 || idx >= currentPosts.size()) {
                showError("Invalid post number.");
                return;
            }
            action.accept(currentPosts.get(idx));
        } catch (NumberFormatException ex) {
            showError("Enter a number.");
        }
    }

    /**
     * Makes this window visible.
     */
    public void showWindow() {
        ViewSupport.onEdt(() -> {
            setVisible(true);
            toFront();
        });
    }

    /**
     * Displays an error message to the user.
     *
     * @param message error text
     */
    public void showError(String message) {
        ViewSupport.showError(this, message);
    }

    /**
     * Reloads the main feed from the controller/model.
     */
    public void refreshFeed() {
        try {
            currentPosts = ctrl().getMainFeed(
                    ctrl().getCurrentUser(), 0, VoteUpModel.FEED_PAGE_LIMIT);
            ViewSupport.fillPosts(feedArea, currentPosts);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged(ModelEvent event) {
        if (isVisible()) {
            refreshFeed();
        }
    }
}
