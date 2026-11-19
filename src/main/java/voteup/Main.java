package voteup;

import voteup.controller.VoteUpController;
import voteup.model.VoteUpModel;
import voteup.view.LoginView;

/**
 * Application entry point for VoteUp.
 */
public class Main {

    /**
     * Launches VoteUp by creating the MVC triad and opening the login view.
     * Full wiring is completed in later process steps; currently constructs
     * objects only.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        VoteUpModel model = new VoteUpModel();
        VoteUpController controller = new VoteUpController(model);
        LoginView loginView = new LoginView(model, controller);
        controller.setView(loginView);
        // loginView.showWindow(); // enabled when view is implemented
    }
}
