package voteup.view;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import voteup.controller.Controller;
import voteup.controller.VoteUpController;
import voteup.model.Model;
import voteup.model.ModelEvent;
import voteup.model.Post;
import voteup.model.VoteUpModel;

/**
 * VoteUp following feed screen.
 */
public class FollowingFeedView extends JFrameView {

    private final PostFeedPanel feedPanel = new PostFeedPanel();
    private List<Post> currentPosts = List.of();

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public FollowingFeedView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp — Following feed");
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
        root.add(ViewSupport.titledHeader("Following feed"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        center.add(new NavBar(ctrl(), this, this::refreshFeed), BorderLayout.NORTH);
        center.add(feedPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton open = new JButton("Open #");
        JButton author = new JButton("Author profile #");
        actions.add(open);
        actions.add(author);
        center.add(actions, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        open.addActionListener(e -> withSelectedPost(ctrl()::showPostPage));
        author.addActionListener(e -> withSelectedPost(p -> ctrl().showProfile(p.getAuthor())));
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
            refreshFeed();
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
     * Reloads the following feed.
     */
    public void refreshFeed() {
        try {
            currentPosts = ctrl().getFollowingFeed(
                    ctrl().getCurrentUser(), 0, VoteUpModel.FEED_PAGE_LIMIT);
            feedPanel.setPosts(currentPosts);
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
