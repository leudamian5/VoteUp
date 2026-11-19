package voteup.view;

import voteup.controller.Controller;
import voteup.model.Model;
import voteup.model.ModelEvent;
import voteup.model.User;

/**
 * VoteUp user profile screen
 * UI layout and behavior implemented in later process increments.
 */
public class ProfileView extends JFrameView {

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public ProfileView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp user profile screen");
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
     * Shows a user profile.
     *
     * @param target profile owner
     */
    public void showProfile(User target) {
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
