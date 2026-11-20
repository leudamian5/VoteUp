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
import voteup.model.Tag;
import voteup.model.VoteUpModel;

/**
 * VoteUp tag feed screen.
 */
public class TagFeedView extends JFrameView {

    private final PostFeedPanel feedPanel = new PostFeedPanel();
    private List<Post> currentPosts = List.of();
    private Tag currentTag = Tag.normalize("java");

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public TagFeedView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp — Tag feed");
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
        root.add(ViewSupport.titledHeader("Tag feed"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        center.add(new NavBar(ctrl(), this, () -> showTag(currentTag)), BorderLayout.NORTH);
        center.add(feedPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton change = new JButton("Change tag");
        JButton open = new JButton("Open #");
        actions.add(change);
        actions.add(open);
        center.add(actions, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        change.addActionListener(e -> {
            String raw = JOptionPane.showInputDialog(this, "Tag name:", currentTag.getName());
            if (raw != null && !raw.isBlank()) {
                showTag(Tag.normalize(raw));
            }
        });
        open.addActionListener(e -> {
            if (currentPosts.isEmpty()) {
                showError("No posts to select.");
                return;
            }
            String input = JOptionPane.showInputDialog(this, "Post number:", "1");
            if (input == null) {
                return;
            }
            try {
                int idx = Integer.parseInt(input.trim()) - 1;
                ctrl().showPostPage(currentPosts.get(idx));
            } catch (Exception ex) {
                showError("Invalid selection.");
            }
        });
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
     * Shows posts for the given tag.
     *
     * @param tag tag to display
     */
    public void showTag(Tag tag) {
        currentTag = tag == null ? Tag.normalize("java") : tag;
        setTitle("VoteUp — Tag feed #" + currentTag.getName());
        try {
            currentPosts = ctrl().getTagFeed(
                    ctrl().getCurrentUser(), currentTag, 0, VoteUpModel.FEED_PAGE_LIMIT);
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
            showTag(currentTag);
        }
    }
}
