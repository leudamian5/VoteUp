package voteup.view;

import java.awt.BorderLayout;
import java.util.List;

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
import voteup.model.User;

/**
 * VoteUp single-post and replies screen.
 */
public class PostPageView extends JFrameView {

    private final JTextArea threadArea = new JTextArea();
    private Post rootPost;
    private List<Post> thread = List.of();

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public PostPageView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp — Post");
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
        root.add(ViewSupport.titledHeader("Post + replies"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        center.add(new NavBar(ctrl(), this, () -> {
            if (rootPost != null) {
                showPost(rootPost);
            }
        }), BorderLayout.NORTH);
        center.add(ViewSupport.postListScroll(threadArea), BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton reply = new JButton("Reply");
        JButton like = new JButton("Like root");
        JButton dislike = new JButton("Dislike root");
        JButton delete = new JButton("Delete root");
        JButton author = new JButton("Author profile");
        actions.add(reply);
        actions.add(like);
        actions.add(dislike);
        actions.add(delete);
        actions.add(author);
        center.add(actions, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        reply.addActionListener(e -> doReply());
        like.addActionListener(e -> engageRoot(EngagementType.LIKE));
        dislike.addActionListener(e -> engageRoot(EngagementType.DISLIKE));
        delete.addActionListener(e -> doDelete());
        author.addActionListener(e -> {
            if (rootPost != null) {
                ctrl().showProfile(rootPost.getAuthor());
            }
        });
    }

    private void doReply() {
        if (rootPost == null) {
            return;
        }
        User me = ctrl().getCurrentUser();
        JTextField text = new JTextField();
        JTextField image = new JTextField();
        Object[] fields = {"Text:", text, "Image URL/path (optional):", image};
        int ok = JOptionPane.showConfirmDialog(this, fields, "Reply", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            String t = text.getText() == null || text.getText().isBlank() ? null : text.getText().trim();
            String img = image.getText() == null || image.getText().isBlank() ? null : image.getText().trim();
            ctrl().createReply(me, rootPost, t, img);
            showPost(rootPost);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void engageRoot(EngagementType type) {
        if (rootPost == null) {
            return;
        }
        try {
            ctrl().applyEngagement(ctrl().getCurrentUser(), rootPost, type);
            showPost(rootPost);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void doDelete() {
        if (rootPost == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(
                this, "Soft-delete this post?", "Delete", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            ctrl().deletePost(ctrl().getCurrentUser(), rootPost);
            ViewSupport.showInfo(this, "Post deleted.");
            ctrl().showMainFeed();
        } catch (Exception ex) {
            showError(ex.getMessage());
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
     * Shows a post thread.
     *
     * @param post root post
     */
    public void showPost(Post post) {
        rootPost = post;
        try {
            thread = ctrl().getPostWithReplies(ctrl().getCurrentUser(), post);
            rootPost = thread.isEmpty() ? post : thread.get(0);
            ViewSupport.fillPosts(threadArea, thread);
            setTitle("VoteUp — Post by @" + rootPost.getAuthor().getUsername());
        } catch (Exception ex) {
            showError(ex.getMessage());
            ctrl().showMainFeed();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged(ModelEvent event) {
        if (isVisible() && rootPost != null) {
            showPost(rootPost);
        }
    }
}
