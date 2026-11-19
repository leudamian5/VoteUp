package voteup.view;

import voteup.controller.Controller;
import voteup.model.Model;
import voteup.model.ModelEvent;
import voteup.model.Tag;

/**
 * VoteUp tag feed screen
 * UI layout and behavior implemented in later process increments.
 */
public class TagFeedView extends JFrameView {

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public TagFeedView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp tag feed screen");
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
     * Shows posts for the given tag.
     *
     * @param tag tag to display
     */
    public void showTag(Tag tag) {
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
