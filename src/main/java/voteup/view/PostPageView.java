package voteup.view;

import voteup.controller.Controller;
import voteup.model.Model;
import voteup.model.ModelEvent;
import voteup.model.Post;

/**
 * VoteUp single-post and replies screen
 * UI layout and behavior implemented in later process increments.
 */
public class PostPageView extends JFrameView {

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public PostPageView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp single-post and replies screen");
    }

    /**
     * Makes this window visible.
     */
    public void showWindow() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Displays an error message to the user.
     *
     * @param message error text
     */
    public void showError(String message) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Shows a post thread.
     *
     * @param post root post
     */
    public void showPost(Post post) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged(ModelEvent event) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
